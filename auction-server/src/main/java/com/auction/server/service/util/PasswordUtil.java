// Lớp tiện ích để mã hóa mật khẩu bằng SHA-256
// Mật khẩu không bao giờ được lưu dưới dạng rõ (plaintext) mà phải được hash trước
// Khi đăng nhập, hash mật khẩu nhập vào rồi so sánh với hash đã lưu
package com.auction.server.service.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class PasswordUtil {

  // Không cho phép tạo instance (utility class)
  private PasswordUtil() {}

  // Mã hóa mật khẩu bằng SHA-256, trả về chuỗi hexadecimal
  public static String hashPassword(String rawPassword) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
      return bytesToHex(hashBytes);
    } catch (NoSuchAlgorithmException e) {
      // SHA-256 là thuật toán chuẩn, không bao giờ xảy ra lỗi này
      throw new RuntimeException("SHA-256 algorithm not available", e);
    }
  }

  // Kiểm tra mật khẩu nhập vào có khớp với hash đã lưu không
  public static boolean verifyPassword(String rawPassword, String storedHash) {
    String hashedInput = hashPassword(rawPassword);
    return hashedInput.equals(storedHash);
  }

  // Chuyển mảng byte sang chuỗi hexadecimal (VD: [0x1A, 0x2B] → "1a2b")
  private static String bytesToHex(byte[] bytes) {
    StringBuilder hexString = new StringBuilder();
    for (byte b : bytes) {
      String hex = Integer.toHexString(0xff & b);
      if (hex.length() == 1) {
        hexString.append('0');
      }
      hexString.append(hex);
    }
    return hexString.toString();
  }
}
