package server.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import org.json.JSONObject;
import server.handler.RequestRouter;

/**
 * Một <b>phiên làm việc TCP</b> với đúng một client: đọc từng dòng JSON, đưa cho {@link
 * RequestRouter}, ghi phản hồi.
 */
public final class ClientSession implements Runnable {

  private final Socket socket;
  private final RequestRouter router;
  private final ClientBroadcaster broadcaster;
  private Long currentUserId = null;

  public ClientSession(Socket socket, RequestRouter router, ClientBroadcaster broadcaster) {
    this.socket = socket;
    this.router = router;
    this.broadcaster = broadcaster;
  }

  @Override
  public void run() {
    PrintWriter out = null;
    try {
      BufferedReader in =
          new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
      out = new PrintWriter(socket.getOutputStream(), true);
      broadcaster.register(out);

      String line;
      while ((line = in.readLine()) != null) {
        String response = router.dispatch(line);
        if (response != null) {
          out.println(response);

          // Nếu là action LOGIN thành công, lưu userId và đăng ký targeted push
          try {
            if (line.contains("\"action\":\"LOGIN\"")
                || line.contains("\"action\":\"LOGIN\"".replace(" ", ""))) {
              JSONObject resJson = new JSONObject(response);
              if ("OK".equals(resJson.optString("status"))) {
                Long userId = resJson.optLong("id", -1);
                if (userId != -1) {
                  currentUserId = userId;
                  broadcaster.registerUser(userId, out);
                }
              }
            }
          } catch (Exception ignored) {
            // Lỗi parse JSON khi kiểm tra login, bỏ qua
          }
        }
      }
    } catch (IOException e) {
      System.out.println("Client disconnected: " + e.getMessage());
    } finally {
      if (out != null) {
        broadcaster.unregister(out);
        if (currentUserId != null) {
          broadcaster.unregisterUser(currentUserId);
        }
      }
      try {
        socket.close();
      } catch (IOException ignored) {
        // Socket đã có thể đóng; bỏ qua
      }
    }
  }
}
