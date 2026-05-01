package com.auction.client.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class SocketConnection {
  private static final String HOST = "localhost";
  private static final int PORT = 8888;

  private static volatile SocketConnection instance;
  private Socket socket;
  private PrintWriter out;
  private BufferedReader in;
  private final BlockingQueue<String> responseQueue = new LinkedBlockingQueue<>();

  // Danh sách các callback nhận push message (chỉ 1 listener thực sự, nhiều callback)
  private final CopyOnWriteArrayList<Consumer<String>> pushListeners = new CopyOnWriteArrayList<>();

  private SocketConnection() throws UnknownHostException, IOException {
    socket = new Socket(HOST, PORT);
    out = new PrintWriter(socket.getOutputStream(), true); // true = auto flush
    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    startSingleListener(); // Chỉ 1 listener duy nhất cho cả app
  }

  /** Khởi động 1 NetworkListener duy nhất đọc từ socket. */
  private void startSingleListener() {
    Thread listener = new Thread(() -> {
      try {
        String line;
        while ((line = in.readLine()) != null) {
          // Xử lý trường hợp "dính dòng": tách các JSON object nếu chúng bị dính sát nhau
          // Ví dụ: {"type":"BID"}{"status":"OK"} -> thành 2 chuỗi riêng biệt
          String[] messages = line.split("(?<=\\})(?=\\{)"); 
          
          for (String msg : messages) {
            if (msg.trim().isEmpty()) continue;
            
            // Phân loại: tin push (có "type") hay response đồng bộ
            if (msg.contains("\"type\":")) {
              for (Consumer<String> listener2 : pushListeners) {
                try { listener2.accept(msg); } catch (Exception ignored) {}
              }
            } else {
              responseQueue.put(msg);
            }
          }
        }
      } catch (Exception e) {
        System.err.println("[SocketConnection] NetworkListener dừng: " + e.getMessage());
      }
    }, "GlobalNetworkListener");
    listener.setDaemon(true);
    listener.start();
  }

  /** Đăng ký nhận push message. Mỗi BidController gọi 1 lần. */
  public void addPushListener(Consumer<String> callback) {
    pushListeners.add(callback);
  }

  /** Hủy đăng ký push listener (gọi khi rời phọng). */
  public void removePushListener(Consumer<String> callback) {
    pushListeners.remove(callback);
  }

  public static SocketConnection getInstance() throws UnknownHostException, IOException {
    if (instance == null) {
      synchronized (SocketConnection.class) {
        if (instance == null || instance.socket.isClosed()) instance = new SocketConnection();
      }
    }
    return instance;
  }

  // Gửi request và chờ response đồng bộ
  public String sendRequest(String jsonRequest) throws IOException {
    out.println(jsonRequest);
    try {
      // Timeout 10 giây
      return responseQueue.poll(10, java.util.concurrent.TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Request interrupted", e);
    }
  }

  // Gửi một chiều — không chờ phản hồi
  public void sendOneWay(String jsonRequest) throws IOException {
    out.println(jsonRequest);
  }

  public BlockingQueue<String> getResponseQueue() {
    return responseQueue;
  }

  public BufferedReader getReader() {
    return in;
  }

  public boolean isConnected() {
    return socket != null && !socket.isClosed();
  }

  public void close() throws IOException {
    socket.close();
  }
}
