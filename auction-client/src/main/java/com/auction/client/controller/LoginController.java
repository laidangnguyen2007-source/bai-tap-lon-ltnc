package com.auction.client.controller;

import com.auction.client.model.AuctionSessionState;
import com.auction.client.service.ServerService;
import com.auction.client.util.FxmlLoader;
import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import server.model.entity.user.User;
import server.model.enums.UserRole;

public class LoginController {

  @FXML private TextField usernameField;
  @FXML private PasswordField passwordField;
  @FXML private Button loginButton;
  @FXML private Button registerButton;
  @FXML private Label errorLabel;

  private final ServerService serverService = new ServerService();

  @FXML
  private void handleLogin(ActionEvent event) {
    String username = usernameField.getText().trim();
    String password = passwordField.getText();

    if (username.isEmpty() || password.isEmpty()) {
      showError("Tên đăng nhập và mật khẩu không được để trống.");
      return;
    }

    User loggedInUser = serverService.login(username, password);

    if (loggedInUser == null) {
      showError("Sai tên đăng nhập hoặc mật khẩu. Vui lòng thử lại.");
      return;
    }

    AuctionSessionState.getInstance().setCurrentUser(loggedInUser);

    try {
      Stage stage = (Stage) loginButton.getScene().getWindow();
      if (loggedInUser.getRole() == UserRole.SELLER) {
        FxmlLoader.navigateTo(stage, "seller-dashboard.fxml", "Online Auction System — Quản Lý Bán Hàng");
      } else {
        FxmlLoader.navigateTo(stage, "auction-list.fxml", "Online Auction System — Danh Sách Đấu Giá");
      }
    } catch (IOException e) {
      showError("Lỗi hệ thống: không thể chuyển màn hình.");
    }
  }

  @FXML
  private void handleGoToRegister(ActionEvent event) {
    try {
      Stage stage = (Stage) registerButton.getScene().getWindow();
      FxmlLoader.navigateTo(stage, "register.fxml", "Online Auction System — Đăng Ký Tài Khoản");
    } catch (IOException e) {
      showError("Không thể mở màn hình đăng ký.");
    }
  }

  private void showError(String message) {
    errorLabel.setText(message);
    errorLabel.setVisible(true);
  }
}
