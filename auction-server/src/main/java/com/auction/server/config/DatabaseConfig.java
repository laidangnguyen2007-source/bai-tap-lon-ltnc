package com.auction.server.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Quản lý kết nối đến MySQL Database theo mô hình Singleton.
 *
 * <p>Singleton Pattern: Chỉ mở 1 kết nối duy nhất tới Database trong suốt vòng đời của Server.
 * Tránh tình trạng mở hàng trăm kết nối dư thừa làm ngốn RAM và gây lỗi "Too many connections".
 *
 * <p>MySQL URL Format: jdbc:mysql://localhost:3306/auction_db → Kết nối tới Server MySQL ở
 * localhost, database auction_db. createDatabaseIfNotExist=true → Tự động tạo database nếu chưa tồn
 * tại.
 */
public class DatabaseConfig {

  // Đường dẫn tới MySQL Server
  private static final String DB_URL =
      "jdbc:mysql://localhost:3306/auction_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

  // Tài khoản mặc định của XAMPP / MySQL Local
  private static final String DB_USER = "root";
  private static final String DB_PASSWORD = "";

  // Instance duy nhất — Singleton Pattern
  private static DatabaseConfig instance;

  // Kết nối duy nhất tới Database
  private Connection connection;

  // Constructor private: Ngăn không cho ai bên ngoài gọi new DatabaseConfig()
  private DatabaseConfig() throws SQLException {
    this.connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    initializeSchema();
  }

  /**
   * Phương thức truy cập Singleton — đảm bảo chỉ có 1 instance tồn tại.
   *
   * <p>Dùng synchronized để tránh Race Condition khi nhiều Thread cùng gọi getInstance() lần đầu.
   */
  public static synchronized DatabaseConfig getInstance() throws SQLException {
    if (instance == null || instance.connection.isClosed()) {
      instance = new DatabaseConfig();
    }
    return instance;
  }

  /** Trả về Connection để các DAO dùng để thực thi SQL. */
  public Connection getConnection() {
    return connection;
  }

  /**
   * Đọc và chạy file schema.sql để tạo bảng khi khởi động lần đầu. CREATE TABLE IF NOT EXISTS đảm
   * bảo lần sau khởi động lại không bị lỗi bảng đã tồn tại.
   */
  private void initializeSchema() throws SQLException {
    try (InputStream is = DatabaseConfig.class.getClassLoader().getResourceAsStream("schema.sql");
        Statement stmt = connection.createStatement()) {

      if (is == null) {
        throw new RuntimeException("Không tìm thấy file schema.sql trong resources/");
      }

      String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);

      // Tách từng câu SQL bằng dấu ; và chạy tuần tự
      for (String statement : sql.split(";")) {
        String trimmed = statement.trim();
        if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
          stmt.execute(trimmed);
        }
      }

    } catch (IOException e) {
      throw new SQLException("Lỗi khi đọc schema.sql: " + e.getMessage(), e);
    }
  }

  /** Đóng kết nối khi Server tắt. Gọi hàm này trong shutdown hook của Server. */
  public void close() throws SQLException {
    if (connection != null && !connection.isClosed()) {
      connection.close();
    }
  }
}
