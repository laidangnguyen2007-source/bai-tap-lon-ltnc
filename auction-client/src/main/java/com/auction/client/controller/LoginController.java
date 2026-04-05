package com.auction.client.controller;

import com.auction.client.model.AuctionSessionState;
import com.auction.client.service.ServerService;
import com.auction.client.util.FxmlLoader;
import com.auction.server.model.entity.user.User;
import com.auction.server.model.enums.UserRole;
import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller cho màn hình Đăng Nhập (login.fxml).
 *
 * <p>Trong mô hình MVC: - View: login.fxml (khai báo giao diện) - Model: AuctionSessionState
 * (lưu user sau khi đăng nhập thành công) - Controller (lớp này): xử lý sự kiện click nút, validate
 * input, gọi ServerService và điều hướng màn hình.
 *
 * <p>Các annotation @FXML cho phép JavaFX runtime tự động "tiêm" (inject) các UI component từ file
 * FXML vào các trường tương ứng trong Controller theo tên id.
 */
public class LoginController {

  // -- @FXML inject từ login.fxml --

  @FXML private TextField usernameField;

  @FXML private PasswordField passwordField;

  @FXML private Button loginButton;

  @FXML private Button registerButton;

  @FXML private Label errorLabel;

  // -- Dependency --

  // Tầng giao tiếp với Server do Thành viên 3 cung cấp (hiện là stub)
  private final ServerService serverService = new ServerService();

  /**
   * Xử lý sự kiện khi người dùng nhấn nút "Đăng Nhập". Luồng: Validate input → Gọi ServerService →
   * Lưu session → Điều hướng màn hình tiếp theo.
   *
   * @param event ActionEvent từ nút loginButton
   */
  @FXML
  private void handleLogin(ActionEvent event) {
    // Bước 1: Lấy và trim dữ liệu đầu vào — loại bỏ khoảng trắng thừa đầu/cuối
    String username = usernameField.getText().trim();
    String password = passwordField.getText();

    // Bước 2: Validate phòng thủ — không cho phép input rỗng
    if (username.isEmpty() || password.isEmpty()) {
      showError("Tên đăng nhập và mật khẩu không được để trống.");
      return;
    }

    // Bước 3: Gọi ServerService để xác thực (TV3 sẽ thay bằng socket call)
    User loggedInUser = serverService.login(username, password);

    // Bước 4: Xử lý kết quả trả về
    if (loggedInUser == null) {
      showError("Sai tên đăng nhập hoặc mật khẩu. Vui lòng thử lại.");
      return;
    }

    // Bước 5: Lưu thông tin người dùng vào session — dùng chung cho toàn bộ ứng dụng
    AuctionSessionState.getInstance().setCurrentUser(loggedInUser);

    // Bước 6: Điều hướng sang màn hình phù hợp theo vai trò người dùng
    try {
      Stage stage = (Stage) loginButton.getScene().getWindow();
      if (loggedInUser.getRole() == UserRole.SELLER) {
        // Seller được chuyển thẳng đến dashboard quản lý
        FxmlLoader.navigateTo(
            stage, "seller-dashboard.fxml", "Online Auction System — Quản Lý Bán Hàng");
      } else {
        // Bidder và Admin xem danh sách đấu giá
        FxmlLoader.navigateTo(
            stage, "auction-list.fxml", "Online Auction System — Danh Sách Đấu Giá");
      }
    } catch (IOException e) {
      showError("Lỗi hệ thống: không thể chuyển màn hình. Chi tiết: " + e.getMessage());
    }
  }

  /**
   * Xử lý sự kiện khi người dùng nhấn "Chưa có tài khoản? Đăng ký".
   *
   * @param event ActionEvent từ nút registerButton
   */
  @FXML
  private void handleGoToRegister(ActionEvent event) {
    try {
      Stage stage = (Stage) registerButton.getScene().getWindow();
      FxmlLoader.navigateTo(stage, "register.fxml", "Online Auction System — Đăng Ký Tài Khoản");
    } catch (IOException e) {
      showError("Không thể mở màn hình đăng ký.");
    }
  }

  /**
   * Hiển thị thông báo lỗi ngay dưới form đăng nhập (inline error, không dùng popup).
   *
   * @param message nội dung thông báo lỗi
   */
  private void showError(String message) {
    errorLabel.setText(message);
    errorLabel.setVisible(true);
  }
}
