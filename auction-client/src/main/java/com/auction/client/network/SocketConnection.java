package com.auction.client.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class SocketConnection {
  private static final String HOST = "localhost";
  private static final int PORT = 8888;

  private static volatile SocketConnection instance;
  private Socket socket;
  private PrintWriter out;
  private BufferedReader in;

  private SocketConnection() throws UnknownHostException, IOException {
    socket = new Socket(HOST, PORT);
    out = new PrintWriter(socket.getOutputStream(), true); // true = auto flush
    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
  }

  public static SocketConnection getInstance() throws UnknownHostException, IOException {
    if (instance == null) {
      synchronized (SocketConnection.class) {
        if (instance == null || instance.socket.isClosed()) instance = new SocketConnection();
      }
    }
    return instance;
  }
  // JSON -> Server, chờ JSON response về
  public synchronized String sendRequest(String jsonRequest) throws IOException {
    out.println(jsonRequest);
    return in.readLine(); // server trả về 1 dòng JSON
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
