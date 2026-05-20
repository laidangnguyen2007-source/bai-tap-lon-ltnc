package server.net;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
  private final Map<Long, PrintWriter> userSessions = new ConcurrentHashMap<>();

  /** Đăng ký writer chung khi client mới vào — gọi từ {@link ClientSession}. */
  public void register(PrintWriter out) {
    connectedClients.add(out);
  }

  /** Gỡ writer chung khi client ngắt. */
  public void unregister(PrintWriter out) {
    connectedClients.remove(out);
  }

  /** Đăng ký mapping userId với PrintWriter khi login thành công. */
  public void registerUser(Long userId, PrintWriter out) {
    userSessions.put(userId, out);
    System.out.println("Broadcaster: User " + userId + " connected. Total active sessions: " + userSessions.size());
  }

  /** Gỡ mapping userId khi disconnect. */
  public void unregisterUser(Long userId) {
    userSessions.remove(userId);
    System.out.println("Broadcaster: User " + userId + " disconnected.");
  }

  /**
   * Gửi <b>một dòng</b> (một JSON string) tới tất cả client.
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

  /**
   * Gửi tin nhắn trực tiếp cho một user cụ thể (targeted push).
   */
  public void sendToUser(Long userId, String message) {
    PrintWriter client = userSessions.get(userId);
    if (client != null) {
      try {
        client.println(message);
        client.flush();
      } catch (Exception e) {
        System.err.println("Lỗi sendToUser (" + userId + "): " + e.getMessage());
      }
    }
  }
}

