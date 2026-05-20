package server.handler;

import org.json.JSONObject;

/**
 * Một hành động (action) mà client gửi lên qua JSON — ví dụ {@code LOGIN}, {@code PLACE_BID}.
 *
 * <p><b>Vì sao cần interface này (hỗ trợ OCP — Open/Closed Principle)?</b>
 *
 * <ul>
 *   <li>Thay vì thêm một {@code case} mới vào {@code switch} khổng lồ mỗi khi có API mới, ta
 *       <b>đăng ký</b> thêm một entry trong {@link java.util.Map Map}{@code <String,
 *       ActionHandler>}.
 *   <li>Lớp điều phối ({@link RequestRouter}) <b>đóng</b> với sửa đổi: không cần biết chi tiết từng
 *       handler; chỉ cần gọi {@link #handle(JSONObject)}.
 *   <li>Phần mở rộng: viết class handler mới + đăng ký — không sửa logic phân tuyến trung tâm.
 * </ul>
 *
 * <p><b>Quy ước giá trị trả về:</b>
 *
 * <ul>
 *   <li>Chuỗi JSON (thường một dòng) — server gửi ngược lại cho đúng client vừa gửi request.
 *   <li>{@code null} — không gửi dòng phản hồi (ví dụ {@code PLACE_BID} thành công: client chờ tin
 *       push {@code BID_UPDATE} qua broadcast).
 * </ul>
 */
@FunctionalInterface
public interface ActionHandler {

  /**
   * Xử lý một request đã parse sẵn thành {@link JSONObject} (đã có trường {@code action}).
   *
   * @param req body JSON; handler con chỉ đọc các field cần thiết
   * @return JSON response một dòng, hoặc {@code null} nếu không phản hồi trực tiếp
   * @throws Exception lỗi nghiệp vụ hoặc kỹ thuật — {@link RequestRouter} sẽ bọc thành {@code
   *     status: ERROR}
   */
  String handle(JSONObject req) throws Exception;
}
