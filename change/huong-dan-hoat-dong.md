# 📘 Hướng Dẫn Hoạt Động Chi Tiết Của Hệ Thống Đấu Giá Trực Tuyến

Tài liệu này trình bày chi tiết về **kiến trúc luồng dữ liệu, các tầng chức năng, và thứ tự gọi hàm** của hệ thống từ lúc người dùng tương tác trên giao diện Client (JavaFX) cho đến khi dữ liệu được xử lý tại Server (TCP Socket) và cập nhật xuống Database (MySQL), cũng như cơ chế phản hồi Realtime (Observer Pattern).

---

## 1. 🏗️ Tổng Quan Kiến Trúc Hệ Thống (Client-Server Architecture)

Hệ thống được thiết kế theo mô hình **Client-Server độc lập**, giao tiếp thời gian thực thông qua kết nối **TCP Socket** với định dạng gói tin **JSON**.

```mermaid
graph TD
    subgraph Client (JavaFX App)
        View[Giao diện FXML]
        Ctrl[JavaFX Controller]
        Service[ServerService Facade]
        NetHandler[Network Handlers]
        SocketClient[SocketConnection / GlobalListener]
    end

    subgraph Mạng TCP/IP
        TCP[TCP Connection - Port 8888]
    end

    subgraph Server (Java TCP Server)
        Session[ClientSession Thread Pool]
        Router[RequestRouter]
        Handler[Action Handlers]
        ServiceServer[AuctionManager / WalletService]
        DAO[DAO Interfaces & JDBC]
        DB[(MySQL Database)]
    end

    View -->|User Action| Ctrl
    Ctrl -->|Call Service| Service
    Service -->|Delegate| NetHandler
    NetHandler -->|Send JSON Request| SocketClient
    SocketClient <==>|TCP Socket| TCP
    TCP <==>|TCP Socket| Session
    Session -->|Dispatch String| Router
    Router -->|Route Action| Handler
    Handler -->|Execute Business| ServiceServer
    ServiceServer -->|Database Queries| DAO
    DAO <==>|SQL Queries| DB
    
    %% Realtime Push Luồng
    ServiceServer -.->|Broadcast Event| Session
    Session -.->|Targeted Push / Broadcast| SocketClient
    SocketClient -.->|Push Message| Service
    Service -.->|Notify Observer| Ctrl
    Ctrl -.->|Platform.runLater| View
```

---

## 2. 🔄 Trình Tự Hoạt Động Chi Tiết Qua Các Chức Năng Mẫu

### 🌟 CHỨC NĂNG 1: Đăng Nhập Hệ Thống (LOGIN)
*Đặc trưng cho luồng: Yêu cầu đồng bộ (Request-Response) - Client gửi yêu cầu và chờ đợi phản hồi từ Server.*

#### Sơ đồ tuần tự gọi hàm:

```mermaid
sequenceDiagram
    autonumber
    actor User as Người dùng
    participant FXML as login.fxml (View)
    participant Ctrl as LoginController
    participant Service as ServerService (Facade)
    participant Net as UserNetworkHandler
    participant Conn as SocketConnection
    participant Session as ClientSession (Server Thread)
    participant Router as RequestRouter
    participant Handler as AuthHandlers
    participant DAO as UserDao / WalletService
    participant DB as MySQL DB

    User->>FXML: Nhập Username/Password & click "Đăng nhập"
    FXML->>Ctrl: handleLogin(ActionEvent)
    Note over Ctrl: Validate dữ liệu đầu vào rỗng/null
    Ctrl->>Service: login(username, password)
    Service->>Net: login(username, password)
    Note over Net: Tạo JSONObject req: {"action": "LOGIN", "username": "...", "password": "..."}
    Net->>Conn: sendRequest(jsonRequest)
    Conn->>Session: Gửi dòng chuỗi raw JSON qua Socket
    Note over Conn: BlockingQueue.poll() chờ tối đa 10s
    
    Session->>Router: dispatch(rawJson)
    Router->>Handler: login(JSONObject)
    Handler->>DAO: findByUsername(username)
    DAO->>DB: SELECT * FROM users WHERE username = ?
    DB-->>DAO: Trả về bản ghi User
    Handler->>Handler: PasswordUtil.verifyPassword(plainText, passwordHash)
    Handler->>DAO: walletService.getWallet(userId)
    DAO->>DB: SELECT * FROM wallets WHERE user_id = ?
    DB-->>DAO: Trả về thông tin ví
    
    Handler-->>Router: Trả về JSONObject res: {"status": "OK", "id": 1, "role": "BIDDER", "totalBalance": 1000000}
    Router-->>Session: Phản hồi JSON dạng chuỗi
    Session-->>Conn: Ghi dòng chuỗi JSON ra socket stream
    Note over Session: Đăng ký userId vào danh sách nhận targeted push (broadcaster.registerUser)
    
    Conn-->>Net: Nhận phản hồi, giải nén JSON
    Net-->>Service: Trả về đối tượng User
    Service-->>Ctrl: Trả về User
    
    alt Đăng nhập thành công
        Ctrl->>Ctrl: Lưu User vào SessionState
        Ctrl->>FXML: FxmlLoader.navigateTo("auction-list.fxml")
    else Đăng nhập thất bại
        Ctrl->>FXML: Hiển thị lỗi ra errorLabel
    end
```

---

### ⚡ CHỨC NĂNG 2: Đặt Giá Trực Tiếp (PLACE_BID)
*Đặc trưng cho luồng: Đặt giá bất đồng bộ (Fire-and-Forget) kết hợp xử lý đồng thời (Concurrency), Khoá Ví tiền (Pessimistic Locking) và Phát tin thời gian thực (Realtime Observer Update).*

#### Sơ đồ tuần tự gọi hàm:

```mermaid
sequenceDiagram
    autonumber
    actor User as Bidder (Người đặt giá)
    participant View as bidding-room.fxml
    participant Ctrl as BiddingRoomController
    participant Service as ServerService
    participant Net as BidNetworkHandler
    participant Conn as SocketConnection
    participant Listener as GlobalNetworkListener (Thread)
    participant Session as ClientSession
    participant Router as RequestRouter
    participant Handler as BiddingHandlers
    participant Wallet as WalletService
    participant RAM as AuctionManager (Singleton RAM)
    participant DAO as BidTransactionDao / AuctionDao
    participant DB as MySQL DB

    User->>View: Nhập giá mới (Ví dụ: 1,500,000đ) & click "Đặt giá"
    View->>Ctrl: handlePlaceBid(ActionEvent)
    Note over Ctrl: Validate giá đặt >= Giá hiện tại + Bước giá tối thiểu
    Ctrl->>View: Hiển thị hộp thoại xác nhận đặt giá
    User->>View: Bấm "Xác nhận"
    Ctrl->>Ctrl: Đặt pendingBidAmount = 1500000, khoá nút đặt giá tránh click nhiều lần
    Ctrl->>Service: placeBid(auctionId, bidderId, 1500000)
    Service->>Net: placeBid(...)
    Note over Net: Tạo JSONObject req: {"action": "PLACE_BID", "auctionId": ..., "amount": 1500000}
    Net->>Conn: sendOneWay(reqString)
    Note over Conn: Gửi dữ liệu đi ngay lập tức và KHÔNG dừng chờ phản hồi.
    
    %% BÊN PHÍA SERVER
    Conn->>Session: Ghi dữ liệu đặt giá qua Socket
    Session->>Router: dispatch(line)
    Router->>Handler: placeBid(req)
    
    Note over Handler: 1. Đồng bộ trạng thái phiên với đồng hồ thực tế
    Handler->>Handler: AuctionStatusSynchronizer.syncWithClock(...)
    
    Note over Handler: 2. Khoá tiền trong ví của người đặt giá (Pessimistic Lock)
    Handler->>Wallet: lockForBid(bidderId, auctionId, 1500000)
    Wallet->>DB: SELECT * FROM wallets WHERE user_id = ? FOR UPDATE (Pessimistic Lock)
    Note over Wallet: Kiểm tra số dư khả dụng (availableBalance >= 1500000).<br/>Chuyển tiền: availableBalance -= 1500000, lockedBalance += 1500000.
    Wallet->>DB: Ghi nhận giao dịch đóng băng tiền ví
    
    Note over Handler: 3. Cập nhật giá cao nhất vào RAM phiên đấu giá (Thread Safe)
    Handler->>RAM: placeBid(auctionId, bidderId, 1500000)
    Note over RAM: Thực hiện trong block synchronized(auction) để chống Race Condition.<br/>Áp dụng Anti-Sniping: Nếu còn dưới 30s cuối, gia hạn thêm 30s.
    RAM-->>Handler: Trả về đối tượng Auction mới nhất
    
    Note over Handler: 4. Hoàn trả tiền cho người dẫn đầu cũ (Outbid Release)
    Handler->>Wallet: releaseForOutbid(prevWinnerId, auctionId, prevWinnerAmount)
    Wallet->>DB: Chuyển ví người cũ: availableBalance += prevAmount, lockedBalance -= prevAmount.
    
    Note over Handler: 5. Lưu kết quả vào cơ sở dữ liệu
    Handler->>DAO: bidTransactionDao.save(BidTransaction)
    Handler->>DAO: auctionDao.update(Auction)
    
    Note over Handler: 6. Phát tín hiệu realtime đến mọi người dùng
    Handler->>Session: broadcaster.broadcast(BID_UPDATE event)
    
    %% REALTIME PHÂN PHỐI VỀ CLIENTS
    Session-->>Conn: Gửi chuỗi {"type": "BID_UPDATE", "bid": {...}} về toàn bộ Client Socket
    Conn->>Listener: Nhận dòng chuỗi
    Listener->>Service: handlePushMessage(line)
    Service->>Ctrl: onBidUpdated(BidTransaction)
    
    Note over Ctrl: Đang ở Background Thread của Network Listener!<br/>Phải bọc trong Platform.runLater()
    Ctrl->>Ctrl: Platform.runLater()
    Ctrl->>View: 1. Vẽ thêm điểm mới trên LineChart (addBidToChart)<br/>2. Cập nhật nhãn giá hiện tại (currentPriceLabel)<br/>3. Thêm dòng lịch sử vào ListView (addBidToHistoryList)<br/>4. Mở khoá nút Đặt giá, hiển thị Toast thông báo thành công
```

---

### 🧠 CHỨC NĂNG 3: Đấu Giá Tự Động (AUTO-BID)
*Cơ chế tự động hóa phức tạp, phối hợp giữa chiến lược đấu giá (Strategy Pattern), hàng đợi ưu tiên (Priority Queue) và xử lý đệ quy.*

```mermaid
flowchart TD
    A[Bắt đầu lượt đấu giá mới] --> B[Người đặt giá thủ công / Tự động khác thành công]
    B --> C[Server lưu BidTransaction mới thành công vào DB]
    C --> D[Gọi AuctionManager.resolveAutoBids#40;auctionId#41;]
    D --> E[Lấy danh sách AutoBidStrategy đã đăng ký]
    E --> F{Có chiến lược tự động nào đang hoạt động và không phải của người vừa dẫn đầu không?}
    
    F -- Không --> G[Kết thúc lượt xử lý, chờ bid tiếp theo]
    F -- Có --> H[Tìm chiến lược AutoBid có độ ưu tiên cao nhất]
    
    Note over H: Độ ưu tiên dựa trên:<br/>1. Giá trị MaxBid cao nhất.<br/>2. Thời gian đăng ký trước (nếu cùng MaxBid).
    
    H --> I[Tính toán bước giá tiếp theo:<br/>nextBid = Math.min#40;currentPrice + increment, maxBid#41;]
    I --> J{Giá tiếp theo có hợp lệ không?<br/>nextBid > currentPrice & <= maxBid}
    
    J -- Không --> K[Vô hiệu hóa AutoBid này] --> F
    J -- Có --> L[Thực hiện đặt giá tự động]
    
    L --> M[Gọi walletService.lockForBid cho người Auto-bid]
    M --> N[Cập nhật Auction State trong RAM]
    N --> O[Hoàn trả ví tiền cho người dẫn đầu trước đó]
    O --> P[Lưu BidTransaction mới tự động vào DB]
    P --> Q[Phát tín hiệu Realtime BID_UPDATE ra toàn hệ thống]
    Q --> D
```

---

### ⏳ CHỨC NĂNG 4: Kết Thúc Phiên & Thanh Toán Đấu Giá (PAID)
*Cơ chế tự động đóng phiên bằng đồng hồ đồng bộ lazy và thanh toán giao dịch tự động.*

#### Quy trình xử lý đóng phiên:

```mermaid
sequenceDiagram
    autonumber
    participant App as Tương tác của người dùng bất kỳ (Xem DS/Đặt giá)
    participant Sync as AuctionStatusSynchronizer
    participant RAM as AuctionManager
    participant Wallet as WalletService
    participant DAO as AuctionDao
    participant Broadcaster as ClientBroadcaster
    participant DB as MySQL DB

    App->>Sync: syncWithClock(auctionDao, walletService, broadcaster)
    Note over Sync: Quét toàn bộ các phiên ở trạng thái OPEN hoặc RUNNING
    
    alt endTime đã qua (Quá giờ đấu giá)
        Sync->>RAM: closeAuction(auctionId)
        RAM-->>Sync: Trả về đối tượng Auction đã đóng
        Sync->>DAO: update(Auction)
        Note over Sync: Cập nhật trạng thái sang FINISHED hoặc PAID
        
        alt Có người đặt giá thắng cuộc
            Sync->>Wallet: settleAuction(Auction, autoBids)
            Wallet->>DB: Thực hiện giao dịch chuyển tiền (ACID Transaction)
            Note over Wallet: 1. Khấu trừ ví người thắng: lockedBalance -= finalPrice, totalBalance -= finalPrice.<br/>2. Cộng ví người bán: totalBalance += finalPrice, availableBalance += finalPrice.<br/>3. Giải phóng số tiền dư còn lại của các tài khoản Auto-Bid.
            
            Sync->>Broadcaster: notifySettlement(Auction)
            Broadcaster-->>DB: Gửi tin nhắn đẩy (Targeted Push)
            Note over Broadcaster: 1. Gửi thông báo "AUCTION_WON" đến người thắng cuộc.<br/>2. Gửi thông báo "SELLER_PAYOUT" đến người bán.
        else Không có ai đặt giá
            Sync->>DAO: Chuyển trạng thái phiên thành FINISHED (Không có người thắng)
        end
    end
```

---

## 3. 📂 Vai Trò Cụ Thể Của Từng Tầng Trong Kiến Trúc

### 🔵 PHẦN CLIENT (Giao diện và Điều phối yêu cầu)

1. **Tầng Giao Diện (View - `.fxml`):** 
   - Định nghĩa layout, định dạng thị giác (bảng biểu, các trường nhập liệu, nút bấm, và đồ thị `LineChart`).
2. **Tầng Điều Khiển (Controller - `*Controller.java`):**
   - Lắng nghe hành động người dùng, kiểm tra validate định dạng cơ bản của dữ liệu (không trống, là số hợp lệ...).
   - Thực thi cập nhật giao diện thông qua `Platform.runLater()` khi nhận thông báo bất đồng bộ từ luồng mạng.
3. **Tầng Dịch Vụ Cổng Kết Nối (Facade Service - `ServerService.java`):**
   - Đóng vai trò là đầu mối (Gateway) duy nhất để các controller tương tác với server, giúp che giấu cấu trúc mạng bên dưới.
   - Nhận thông tin trực tiếp từ luồng listener và phân phối sự kiện cho các `AuctionObserver` đang quan sát.
4. **Tầng Xử Lý Mạng (Network Handlers - `*NetworkHandler.java`):**
   - Chuyển đổi dữ liệu đối tượng Java thành định dạng chuỗi JSON thô để gửi qua mạng và ngược lại.
5. **Tầng Kết Nối Thô (Socket Connection - `SocketConnection.java`):**
   - Quản lý vòng đời socket TCP. Khởi động một luồng nền độc lập (`GlobalNetworkListener`) liên tục đọc dữ liệu từ server và đẩy phản hồi về các hàng đợi hoặc trực tiếp qua các callback.

---

### 🟢 PHẦN SERVER (Xử lý Nghiệp vụ và Lưu trữ)

1. **Tầng Giao Tiếp Socket (Network Session - `ClientSession.java`):**
   - Chạy trên một luồng riêng biệt của Thread Pool để quản lý một kết nối client duy nhất, đảm bảo tính song song khi có hàng trăm client truy cập.
2. **Tầng Điều Phối Yêu Cầu (Router - `RequestRouter.java`):**
   - Phân tích trường `action` trong chuỗi JSON nhận được để điều hướng đến đúng Handler chức năng xử lý (Áp dụng Open-Closed Principle).
3. **Tầng Điều Khiển Nghiệp Vụ (Action Handlers - `*Handlers.java`):**
   - Tiếp nhận JSON từ Router, bóc tách các tham số, gọi các dịch vụ nghiệp vụ cần thiết và chuẩn bị JSON phản hồi.
4. **Tầng Dịch Vụ Cốt Lõi (Core Services - `AuctionManager`, `WalletService`):**
   - Chứa toàn bộ logic nghiệp vụ thực tế của hệ thống: Đặt giá, quản lý tự động trả giá, khóa/mở ví tiền, đóng/mở phiên đấu giá.
   - Đảm bảo tính an toàn dữ liệu khi có tranh chấp tài nguyên (Concurrency control & Transaction lock).
5. **Tầng Truy Xuất Dữ Liệu (DAO Layer - `*Dao` & `Jdbc*Dao`):**
   - Thực thi các câu lệnh SQL để đọc/ghi dữ liệu từ Database MySQL. Hoàn toàn cách ly logic nghiệp vụ khỏi câu lệnh SQL.
6. **Cơ Sở Dữ Liệu (Database - MySQL):**
   - Nơi lưu trữ vĩnh viễn dữ liệu người dùng, ví tiền, sản phẩm, và mọi giao dịch đặt giá.
