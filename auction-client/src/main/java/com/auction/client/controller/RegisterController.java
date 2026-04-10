package com.auction.client.controller;

import com.auction.client.service.ServerService;
import com.auction.client.util.FxmlLoader;
import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;

/**
 * Controller cho màn hình Đăng Ký tài khoản (register.fxml).
 *
 * <p>Cho phép người dùng đăng ký với vai trò Bidder hoặc Seller. Nếu chọn Seller, hiển thị thêm
 * trường nhập tên cửa hàng (shopName). Validate toàn bộ input trước khi gửi lên server.
 */
public class RegisterController {

  // -- @FXML inject từ register.fxml --

  @FXML private TextField usernameField;

  @FXML private PasswordField passwordField;

  @FXML private PasswordField confirmPasswordField;

  @FXML private TextField emailField;

  @FXML private RadioButton bidderRadio;

  @FXML private RadioButton sellerRadio;

  @FXML private TextField shopNameField;

  @FXML private Label shopNameLabel;

  @FXML private Button registerButton;

  @FXML private Button backToLoginButton;

  @FXML private Label errorLabel;

  @FXML private Label successLabel;

  // -- Dependency --

  private final ServerService serverService = new ServerService();

  /**
   * Được JavaFX tự động gọi sau khi load FXML xong. Dùng để thiết lập trạng thái ban đầu của các
   * component (GreGroup, ẩn/hiện trường tùy chọn...).
   */
  @FXML
  public void initialize() {
    // Nhóm 2 RadioButton vào ToggleGroup để đảm bảo chỉ chọn được 1 lúc
    ToggleGroup roleGroup = new ToggleGroup();
    bidderRadio.setToggleGroup(roleGroup);
    sellerRadio.setToggleGroup(roleGroup);
    bidderRadio.setSelected(true); // Mặc định chọn Bidder

    // Ẩn trường shopName — chỉ hiển thị khi chọn Seller
    shopNameField.setVisible(false);
    shopNameLabel.setVisible(false);

    // Lắng nghe thay đổi lựa chọn role để hiện/ẩn shopName
    sellerRadio
        .selectedProperty()
        .addListener(
            (observable, wasSelected, isNowSelected) -> {
              shopNameField.setVisible(isNowSelected);
              shopNameLabel.setVisible(isNowSelected);
            });
  }

  /**
   * Xử lý sự kiện khi nhấn nút "Đăng Ký". Validate input toàn diện trước khi gửi lên server.
   *
   * @param event ActionEvent từ registerButton
   */
  @FXML
  private void handleRegister(ActionEvent event) {
    hideMessages();

    // Lấy dữ liệu từ form
    String username = usernameField.getText().trim();
    String password = passwordField.getText();
    String confirmPassword = confirmPasswordField.getText();
    String email = emailField.getText().trim();
    String role = sellerRadio.isSelected() ? "SELLER" : "BIDDER";
    String shopName = shopNameField.getText().trim();

    // Validate: kiểm tra các trường bắt buộc không được rỗng
    if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
      showError("Vui lòng điền đầy đủ tên đăng nhập, mật khẩu và email.");
      return;
    }

    // Validate: mật khẩu phải đủ dài (tối thiểu 6 ký tự)
    if (password.length() < 6) {
      showError("Mật khẩu phải có ít nhất 6 ký tự.");
      return;
    }

    // Validate: xác nhận mật khẩu phải khớp
    if (!password.equals(confirmPassword)) {
      showError("Mật khẩu xác nhận không khớp.");
      return;
    }

    // Validate: email phải có định dạng cơ bản (chứa @)
    if (!email.contains("@")) {
      showError("Email không hợp lệ.");
      return;
    }

    // Validate: nếu đăng ký Seller thì phải có tên cửa hàng
    if ("SELLER".equals(role) && shopName.isEmpty()) {
      showError("Vui lòng nhập tên cửa hàng cho tài khoản Seller.");
      return;
    }

    // Gọi ServerService để đăng ký (TV3 sẽ thay stub bằng socket call thực)
    boolean success = serverService.register(username, password, email, role, shopName);

    if (success) {
      showSuccess("Đăng ký thành công! Bạn có thể đăng nhập ngay bây giờ.");
    } else {
      showError("Đăng ký thất bại. Tên đăng nhập hoặc email đã tồn tại.");
    }
  }

  /**
   * Quay lại màn hình đăng nhập.
   *
   * @param event ActionEvent từ backToLoginButton
   */
  @FXML
  private void handleBackToLogin(ActionEvent event) {
    try {
      Stage stage = (Stage) backToLoginButton.getScene().getWindow();
      FxmlLoader.navigateTo(stage, "login.fxml", "Online Auction System — Đăng Nhập");
    } catch (IOException e) {
      showError("Không thể quay lại màn hình đăng nhập.");
    }
  }

  private void showError(String message) {
    errorLabel.setText(message);
    errorLabel.setVisible(true);
    successLabel.setVisible(false);
  }

  private void showSuccess(String message) {
    successLabel.setText(message);
    successLabel.setVisible(true);
    errorLabel.setVisible(false);
  }

  private void hideMessages() {
    errorLabel.setVisible(false);
    successLabel.setVisible(false);
  }
}
