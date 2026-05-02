package com.auction.server.net;

import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Phát tin <b>real-time</b> tới mọi client đang giữ kết nối TCP (ví dụ cập nhật giá đấu, reset
 * phiên).
 *
 * <p><b>SRP:</b> Chỉ lo danh sách {@link PrintWriter} và vòng lặp gửi — không xen vào parse JSON
 * hay DAO.
 *
 * <p><b>Thread-safety:</b> Dùng {@link CopyOnWriteArrayList} vì:
 *
 * <ul>
 *   <li>Luồng session: thêm/xóa writer khi client connect/disconnect.
 *   <li>Luồng handler: có thể gọi {@link #broadcast(String)} đồng thời khi nhiều người đặt giá.
 * </ul>
 *
 * Copy-on-write phù hợp khi <b>đọc/broadcast nhiều</b> hơn so với thêm/xóa (đúng với phòng đấu
 * giá).
 */
public final class ClientBroadcaster {

  private final List<PrintWriter> connectedClients = new CopyOnWriteArrayList<>();

  /** Đăng ký writer khi client mới vào — gọi từ {@link ClientSession}. */
  public void register(PrintWriter out) {
    connectedClients.add(out);
  }

  /** Gỡ writer khi client ngắt — tránh rò rỉ và gửi vào socket đã đóng. */
  public void unregister(PrintWriter out) {
    connectedClients.remove(out);
  }

  /**
   * Gửi <b>một dòng</b> (một JSON string) tới tất cả client. Client JavaFX đọc dòng và parse.
   *
   * <p>Lỗi từng client (socket hỏng) chỉ log, không làm sập server.
   */
  public void broadcast(String message) {
    for (PrintWriter client : connectedClients) {
      try {
        client.println(message);
        client.flush();
      } catch (Exception e) {
        System.err.println("Lỗi broadcast: " + e.getMessage());
      }
    }
  }
}
