package com.auction.client.util;
 
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;
 
/**
 * Tiện ích hiển thị thông báo "Toast" (thông báo nhỏ tự biến mất) trong JavaFX.
 * Giúp người dùng nhận biết các sự kiện Real-time mà không bị gián đoạn trải nghiệm.
 */
public class NotificationUtils {
 
    /**
     * Hiển thị một thông báo Toast.
     * 
     * @param stage Stage hiện tại để gắn Popup vào
     * @param message Nội dung thông báo
     * @param isError Nếu true, hiện màu đỏ, nếu false hiện màu xanh thành công
     */
    public static void showToast(Stage stage, String message, boolean isError) {
        // Đảm bảo code chạy trên UI Thread của JavaFX
        Platform.runLater(() -> {
            Popup popup = new Popup();
            popup.setAutoFix(true);
 
            Label label = new Label(message);
            label.setWrapText(true);
            label.setMaxWidth(300);
            label.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
 
            HBox container = new HBox(label);
            container.setPadding(new Insets(15, 25, 15, 25));
            container.setAlignment(Pos.CENTER);
            
            // Thiết lập màu sắc dựa trên loại thông báo
            String bgColor = isError ? "#e74c3c" : "#2ecc71";
            container.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 10; -fx-border-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 5);");
 
            popup.getContent().add(container);
 
            // Vị trí: Ở góc trên bên phải màn hình
            popup.show(stage);
            popup.setX(stage.getX() + stage.getWidth() - container.getWidth() - 40);
            popup.setY(stage.getY() + 60);
 
            // Hiệu ứng: Hiện lên (FadeIn) -> Chờ 3s -> Biến mất (FadeOut) -> Đóng Popup
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), container);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
 
            PauseTransition stay = new PauseTransition(Duration.seconds(3));
 
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), container);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> popup.hide());
 
            new SequentialTransition(fadeIn, stay, fadeOut).play();
        });
    }
    
    /**
     * Tiện ích hiển thị thông báo thành công.
     */
    public static void showSuccess(Stage stage, String message) {
        showToast(stage, "✅ " + message, false);
    }
 
    /**
     * Tiện ích hiển thị thông báo lỗi/cảnh báo.
     */
    public static void showError(Stage stage, String message) {
        showToast(stage, "⚠️ " + message, true);
    }

    /**
     * Nhuộm màu cho hộp thoại Dialog để đồng bộ với giao diện Dark Theme của ứng dụng.
     * Phương thức này nạp file styles.css vào DialogPane của Dialog.
     * 
     * @param dialog Đối tượng Dialog/Alert cần được style lại
     */
    public static void styleDialog(javafx.scene.control.Dialog<?> dialog) {
        if (dialog == null) return;
        javafx.scene.control.DialogPane dialogPane = dialog.getDialogPane();
        // Nạp file CSS của ứng dụng
        String css = NotificationUtils.class.getResource("/com/auction/client/css/styles.css").toExternalForm();
        dialogPane.getStylesheets().add(css);
    }

    /**
     * Giữ lại tên cũ để không làm lỗi code ở các chỗ khác, nhưng gọi sang styleDialog.
     */
    public static void styleAlert(javafx.scene.control.Alert alert) {
        styleDialog(alert);
    }
}
