package com.auction.server.net;

import com.auction.server.handler.RequestRouter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Một <b>phiên làm việc TCP</b> với đúng một client: đọc từng dòng JSON, đưa cho {@link
 * RequestRouter}, ghi phản hồi.
 *
 * <p><b>SRP:</b> Lớp {@link com.auction.server.Server} (entry point) chỉ lo chấp nhận socket và bơm
 * thread; còn chi tiết đọc/ghi/đăng ký broadcast nằm ở đây — dễ unit test hoặc thay protocol sau
 * này (ví dụ length-prefix thay vì một-dòng-một-JSON).
 *
 * <p><b>Vòng đời:</b> Khi client kết nối, session đăng ký {@link PrintWriter} vào {@link
 * ClientBroadcaster}. Khi ngắt (IOException hoặc EOF), {@code finally} gỡ writer để không broadcast
 * vào socket chết (tránh rò bộ nhớ và exception spam).
 */
public final class ClientSession implements Runnable {

  private final Socket socket;
  private final RequestRouter router;
  private final ClientBroadcaster broadcaster;

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
        }
      }
    } catch (IOException e) {
      System.out.println("Client disconnected: " + e.getMessage());
    } finally {
      if (out != null) {
        broadcaster.unregister(out);
      }
      try {
        socket.close();
      } catch (IOException ignored) {
        // Socket đã có thể đóng; bỏ qua
      }
    }
  }
}
