package com.auction.client;

/**
 * Lớp khởi động thực sự của ứng dụng JavaFX.
 *
 * <p>Tại sao cần lớp này thay vì gọi MainApp trực tiếp? Kể từ Java 11+, khi đóng gói thành JAR, nếu
 * lớp main kế thừa {@code javafx.application.Application} mà không khai báo module JavaFX đúng
 * cách, JVM sẽ báo lỗi "JavaFX runtime components are missing". Lớp Launcher không kế thừa
 * Application nên JVM load được bình thường, sau đó JavaFX runtime mới được kéo vào.
 *
 * <p>IntelliJ IDEA cũng sử dụng lớp này làm điểm khởi chạy (xem .idea/workspace.xml).
 */
public class Launcher {

  public static void main(String[] args) {
    MainApp.main(args);
  }
}
