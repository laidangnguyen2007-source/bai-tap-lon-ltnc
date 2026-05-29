package com.auction.client.util;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Tiện ích hỗ trợ load file FXML và chuyển màn hình trong ứng dụng.
 *
 * <p>Lớp này đóng gói logic load FXML để tránh lặp code ở các Controller. Theo nguyên tắc DRY
 * (Don't Repeat Yourself), thay vì mỗi Controller tự viết FXMLLoader, tất cả đều gọi qua đây.
 *
 * <p>Quy ước đặt tên FXML: tất cả file .fxml nằm trong /resources/com/auction/client/fxml/ và tên
 * file được truyền vào phương thức navigateTo() không cần đường dẫn đầy đủ.
 */
public final class FxmlLoader {

  // Đường dẫn gốc của tất cả file FXML trong classpath
  private static final String FXML_BASE_PATH = "/com/auction/client/fxml/";

  // Lớp tiện ích thuần túy, không cho phép khởi tạo
  private FxmlLoader() {
    throw new UnsupportedOperationException("Utility class — không khởi tạo trực tiếp");
  }

  /**
   * Chuyển sang màn hình mới bằng cách load file FXML và cập nhật scene của stage hiện tại.
   *
   * @param stage Stage cần được cập nhật (thường là stage hiện tại lấy từ event)
   * @param fxmlFileName tên file FXML (ví dụ: "auction-list.fxml")
   * @param title tiêu đề cửa sổ sau khi chuyển màn hình
   * @throws IOException nếu file FXML không tồn tại hoặc bị lỗi syntax
   */
  public static void navigateTo(Stage stage, String fxmlFileName, String title) throws IOException {
    String fullPath = FXML_BASE_PATH + fxmlFileName;
    FXMLLoader loader = new FXMLLoader(FxmlLoader.class.getResource(fullPath));

    if (loader.getLocation() == null) {
      throw new IOException("Không tìm thấy file FXML: " + fullPath);
    }

    Parent root = loader.load();
    Scene scene = new Scene(root);

    // Load stylesheet chung cho toàn bộ ứng dụng
    String cssPath =
        FxmlLoader.class.getResource("/com/auction/client/css/styles.css").toExternalForm();
    scene.getStylesheets().add(cssPath);

    stage.setTitle(title);
    stage.setScene(scene);
    stage.show();
  }

  /**
   * Chỉ load FXML và trả về FXMLLoader để Controller bên ngoài có thể lấy controller instance. Dùng
   * khi cần truyền dữ liệu vào Controller đích trước khi hiển thị màn hình.
   *
   * @param fxmlFileName tên file FXML (ví dụ: "bidding-room.fxml")
   * @return FXMLLoader đã được cấu hình (chưa load)
   */
  public static FXMLLoader createLoader(String fxmlFileName) {
    String fullPath = FXML_BASE_PATH + fxmlFileName;
    return new FXMLLoader(FxmlLoader.class.getResource(fullPath));
  }
}
