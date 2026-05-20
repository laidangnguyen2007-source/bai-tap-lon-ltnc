package server.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/*
 * Đây là 1 lớp quản lý kết nối từ Java xuống MySQL. Singleton Pattern: Chỉ mở 1 kết nối duy nhất
 * tới Database trong suốt vòng đời của Server. Cái gì cần thao tác dữ liệu thì dùng chung đường ống
 * kết nối này. Tránh tình trạng mở hàng trăm kết nối dư thừa làm ngốn RAM và gây lỗi
 * "Too many connections". MySQL URL Format: jdbc:mysql://localhost:3306/auction_db → Kết nối tới
 * Server MySQL ở localhost, database auction_db. createDatabaseIfNotExist=true → Tự động tạo
 * database nếu chưa tồn tại.
 */
public class DatabaseConfig {

  // Đường dẫn tới MySQL Server
  private static final String DB_URL =
      "jdbc:mysql://localhost:3306/auction_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
  // Tài khoản mặc định của MySQL Local
  private static final String DB_USER = "root";
  private static final String DB_PASSWORD = "1234";

  // Instance duy nhất — Singleton Pattern
  private static volatile DatabaseConfig instance;

  // Kết nối duy nhất tới Database
  private Connection connection;

  // Constructor private: Ngăn không cho ai bên ngoài gọi new DatabaseConfig()
  private DatabaseConfig() throws SQLException {
    this.connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    initializeSchema();
    // hàm getConnection() của thư viện DriverManager sẽ kiểm tra nội bộ và tạo ra Object Connection
    // nối thẳng tới MySQL Server
  }

  // Double-Checked Locking
  public static DatabaseConfig getInstance() throws SQLException {
    DatabaseConfig result = instance;
    // Lệnh isClosed() đóng vai trò cơ chế Tự phục hồi (Self-Healing) phòng khi mất kết nối
    if (result == null || result.connection.isClosed()) {
      synchronized (DatabaseConfig.class) {
        result = instance;
        if (result == null || result.connection.isClosed()) {
          instance = result = new DatabaseConfig();
        }
      }
    }
    return result;
  }

  // Trả về Connection để các DAO dùng để thực thi SQL.
  public Connection getConnection() {
    return connection;
  }

  // Đọc và chạy file schema.sql để tạo bảng khi khởi động lần đầu, rồi chia dòng gửi đến database.
  // CREATE TABLE IF NOT EXISTS đảm bảo lần sau khởi động lại không bị lỗi bảng đã tồn tại.
  private void initializeSchema() throws SQLException {
    try (InputStream is = DatabaseConfig.class.getClassLoader().getResourceAsStream("schema.sql");
        Statement stmt = connection.createStatement()) {

      if (is == null) {
        throw new RuntimeException("Không tìm thấy file schema.sql trong resources/");
      }

      String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);

      // Loại bỏ toàn bộ comment (-- ...) để tránh lỗi startsWith("--") khi cắt chuỗi
      sql = sql.replaceAll("--.*", "");

      // Tách từng câu SQL bằng dấu ; và chạy tuần tự
      for (String statement : sql.split(";")) {
        String trimmed = statement.trim();
        if (!trimmed.isEmpty()) {
          try {
            stmt.execute(trimmed);
          } catch (SQLException e) {
            // Bỏ qua các lỗi "đã tồn tại" để hỗ trợ chạy lại script migration nhiều lần
            int errorCode = e.getErrorCode();
            String cmd = trimmed.toUpperCase();

            // 1050: Table already exists | 1060: Duplicate column name | 1061: Duplicate key name
            boolean isAlreadyExists = (errorCode == 1050 || errorCode == 1060 || errorCode == 1061);
            boolean isSchemaChange =
                (cmd.contains("CREATE TABLE")
                    || cmd.contains("ALTER TABLE")
                    || cmd.contains("CREATE INDEX"));

            if (isAlreadyExists && isSchemaChange) {
              System.out.println(
                  "Lưu ý: Bỏ qua lệnh (đã tồn tại): "
                      + trimmed.substring(0, Math.min(trimmed.length(), 30))
                      + "...");
            } else {
              throw e;
            }
          }
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
