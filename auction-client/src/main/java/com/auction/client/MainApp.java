package com.auction.client;

import com.auction.client.util.FxmlLoader;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Điểm khởi động (Entry Point) của ứng dụng Client JavaFX.
 *
 * <p>Trong JavaFX, lớp chính PHẢI kế thừa {@link Application} và override phương thức
 * {@link #start(Stage)}. JavaFX runtime tự động gọi start() sau khi khởi tạo xong JavaFX toolkit
 * và tạo primaryStage.
 *
 * <p>Luồng khởi động: main() → launch() → init() → start(primaryStage) → [ứng dụng chạy] →
 * stop().
 */
public class MainApp extends Application {

  @Override
  public void start(Stage primaryStage) throws Exception {
    // Màn hình đầu tiên luôn là màn hình đăng nhập
    FxmlLoader.navigateTo(primaryStage, "login.fxml", "Online Auction System — Đăng Nhập");

    // Cố định kích thước cửa sổ không cho resize tùy tiện ảnh hưởng layout
    primaryStage.setResizable(true);
    primaryStage.setMinWidth(900);
    primaryStage.setMinHeight(620);
  }

  public static void main(String[] args) {
    // launch() là phương thức static của Application, khởi động toàn bộ JavaFX runtime
    launch(args);
  }
}
