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

public class RegisterController {

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

  private final ServerService serverService = new ServerService();

  @FXML
  public void initialize() {
    ToggleGroup roleGroup = new ToggleGroup();
    bidderRadio.setToggleGroup(roleGroup);
    sellerRadio.setToggleGroup(roleGroup);
    bidderRadio.setSelected(true);

    shopNameField.setVisible(false);
    shopNameLabel.setVisible(false);

    sellerRadio.selectedProperty().addListener((observable, wasSelected, isNowSelected) -> {
      shopNameField.setVisible(isNowSelected);
      shopNameLabel.setVisible(isNowSelected);
    });
  }

  @FXML
  private void handleRegister(ActionEvent event) {
    hideMessages();
    String username = usernameField.getText().trim();
    String password = passwordField.getText();
    String confirmPassword = confirmPasswordField.getText();
    String email = emailField.getText().trim();
    String role = sellerRadio.isSelected() ? "SELLER" : "BIDDER";
    String shopName = shopNameField.getText().trim();

    if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
      showError("Vui lòng điền đầy đủ thông tin.");
      return;
    }

    if (!password.equals(confirmPassword)) {
      showError("Mật khẩu xác nhận không khớp.");
      return;
    }

    boolean success = serverService.register(username, password, email, role, shopName);

    if (success) {
      showSuccess("Đăng ký thành công!");
    } else {
      showError("Đăng ký thất bại.");
    }
  }

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
