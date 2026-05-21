# Báo Cáo Sửa Lỗi: Đơ Giao Diện (UI Freeze) Trong Màn Hình Ví

## 1. Mô tả hiện tượng
Khi người dùng truy cập vào giao diện ví và nhấn vào một nút bất kỳ (ví dụ: nút "Làm mới" hoặc "Quay lại"), toàn bộ giao diện của ứng dụng JavaFX bị đơ cứng (Not Responding) và không thể thao tác được trong khoảng 10 giây. Sau 10 giây, giao diện mới phản hồi trở lại.

## 2. Nguyên nhân gốc rễ (Root Cause)
Lỗi này xuất phát từ một hiện tượng **Race Condition (Xung đột luồng)** kết hợp với lỗi **phân loại nhầm bản tin (Message Misclassification)** cực kỳ phức tạp trong phần mạng (`SocketConnection`) của hệ thống:

1. **Lấy dữ liệu ngầm:** Khi người dùng vào giao diện ví hoặc nhấn "Làm mới", hệ thống tạo ra một luồng ngầm (background thread) gọi là `WalletDataLoader` để gửi yêu cầu lấy lịch sử giao dịch ví (`GET_WALLET_TRANSACTIONS`) từ Server.
2. **Lỗi phân loại bản tin (Misclassification):** Server trả về danh sách giao dịch dưới dạng JSON. Trong mỗi giao dịch có chứa một trường `"type"` (ví dụ: `"type":"BID_LOCK"`). Do class `SocketConnection.java` lúc đó đang dùng câu lệnh rất thô sơ `if (msg.contains("\"type\":"))` để phân biệt thông báo Push (Real-time) và kết quả Response đồng bộ, chuỗi JSON trả về chứa `"type"` nên đã bị hệ thống hiểu lầm toàn bộ là một bản tin Push. Hệ quả là nó **không được đẩy vào `responseQueue`**.
3. **Luồng ngầm bị treo (Thread Blocked):** Luồng ngầm `WalletDataLoader` lúc này đang gọi `responseQueue.poll(10, TimeUnit.SECONDS)` để chờ kết quả. Do kết quả đã bị hệ thống bỏ qua (do hiểu nhầm ở bước 2), luồng ngầm này bị treo và phải chờ hết thời gian timeout là 10 giây.
4. **Luồng giao diện bị bỏ đói (Starvation):** Trong lúc luồng ngầm bị treo, nếu người dùng ấn nút "Quay lại", hệ thống chuyển sang màn hình danh sách đấu giá. **Luồng giao diện (UI Thread)** sẽ gửi yêu cầu lấy danh sách đấu giá (`GET_ALL_AUCTIONS`) và cũng gọi `responseQueue.poll(...)` để chờ.
5. **Cướp kết quả (Response Stealing):** Khi Server trả kết quả của `GET_ALL_AUCTIONS` về và đẩy vào `responseQueue`, luồng ngầm (đang bị treo và trực sẵn ở hàng đợi) đã cướp mất kết quả này.
6. **Hệ quả cuối cùng:** UI Thread (luồng hiển thị giao diện) không bao giờ nhận được kết quả của chính nó. Do đó, toàn bộ giao diện JavaFX bị treo cứng trong suốt 10 giây chờ timeout.

## 3. Giải pháp khắc phục
Để khắc phục vấn đề này, cơ chế phân loại bản tin trong luồng `GlobalNetworkListener` của class `SocketConnection.java` đã được thay đổi. Thay vì dùng hàm `contains` thô sơ của String, hệ thống giờ đây thực sự phân tích (parse) cú pháp JSON để kiểm tra chính xác các key ở cấp độ ngoài cùng.

**Cụ thể:**
- Sử dụng `JSONParser` để parse chuỗi nhận được từ Server thành `JSONObject`.
- Chỉ những bản tin nào thoả mãn điều kiện có chứa khóa `"type"` và không chứa khóa `"status"`: `json.containsKey("type") && !json.containsKey("status")` thì mới được phân loại là thông báo Push Real-time.
- Mọi bản tin Response thông thường (đều chứa `status` là `OK` hoặc `ERROR`) sẽ được trả về đúng `responseQueue` để giải phóng các luồng đang gọi.

**Sự thay đổi trong code (`SocketConnection.java`):**
```java
// CODE CŨ (Bị lỗi)
if (msg.contains("\"type\":")) {
    // Xử lý Push Message
} else {
    responseQueue.put(msg);
}

// CODE MỚI (Đã sửa)
try {
    JSONObject json = (JSONObject) new JSONParser().parse(msg);
    if (json.containsKey("type") && !json.containsKey("status")) {
        // Xử lý Push Message
    } else {
        responseQueue.put(msg);
    }
} catch (Exception e) {
    responseQueue.put(msg);
}
```

Bằng cách sử dụng cách phân tích cấu trúc này, không còn tình trạng luồng ngầm bị treo vô cớ hay ăn cắp kết quả của luồng giao diện. Hệ thống sẽ luôn hoạt động mượt mà ngay cả khi người dùng thực hiện liên tục các thao tác điều hướng.
