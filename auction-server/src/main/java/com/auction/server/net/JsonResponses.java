package com.auction.server.net;

import org.json.JSONObject;

/**
 * Tập hợp <b>factory</b> cho các payload JSON thống nhất giữa server và client.
 *
 * <p><b>SRP (Single Responsibility):</b> Mọi chỗ cần trả lỗi hoặc format chuẩn đều gọi vào đây,
 * tránh nhân bản {@code new JSONObject()} + {@code put("status", "ERROR")} rải rác trong nhiều
 * handler — giảm lỗi copy-paste và dễ đổi contract một chỗ.
 */
public final class JsonResponses {

  private JsonResponses() {}

  /** Phản hồi lỗi thống nhất: client có thể đọc {@code message} để hiển thị cho người dùng. */
  public static String error(String message) {
    JSONObject res = new JSONObject();
    res.put("status", "ERROR");
    res.put("message", message != null ? message : "Unknown error!");
    return res.toString();
  }
}
