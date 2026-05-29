package com.auction.client.util;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.stage.PopupWindow;
import javafx.stage.Window;

/**
 * Đồng bộ chiều ngang {@link ListView} trong popup của {@link ComboBox} với chính ô ComboBox.
 *
 * <p>Modena đôi khi để popup <b>rộng hơn</b> ô (theo nội dung dài nhất + scrollbar), nên hai mép
 * trên không thẳng hàng với viền ComboBox — trông “bậc”/lệch ở góc nối. Gắn listener khi mở popup
 * rồi ép {@code pref/min/max width} của {@link ListView} khớp {@code combo.getWidth()}.
 */
public final class ComboBoxPopupWidthSync {

  private ComboBoxPopupWidthSync() {}

  /** Đăng ký đồng bộ width mỗi lần popup của {@code combo} được mở. */
  public static void install(ComboBox<?> combo) {
    if (combo == null) {
      return;
    }
    combo
        .showingProperty()
        .addListener(
            (obs, wasShowing, showing) -> {
              if (Boolean.TRUE.equals(showing)) {
                Platform.runLater(() -> syncWidth(combo));
              }
            });
  }

  private static void syncWidth(ComboBox<?> combo) {
    double w = combo.getWidth();
    if (w <= 0) {
      w = combo.getPrefWidth();
    }
    if (w <= 0) {
      return;
    }
    Window owner = combo.getScene() != null ? combo.getScene().getWindow() : null;
    final double target = w;
    for (Window win : Window.getWindows()) {
      if (!(win instanceof PopupWindow pw) || !pw.isShowing()) {
        continue;
      }
      if (owner != null && pw.getOwnerWindow() != owner) {
        continue;
      }
      Scene sc = pw.getScene();
      if (sc == null) {
        continue;
      }
      ListView<?> lv = findListView(sc.getRoot());
      if (lv != null) {
        lv.setPrefWidth(target);
        lv.setMinWidth(target);
        lv.setMaxWidth(target);
        return;
      }
    }
  }

  private static ListView<?> findListView(Node n) {
    if (n instanceof ListView<?> lv) {
      return lv;
    }
    if (n instanceof Parent p) {
      for (Node c : p.getChildrenUnmodifiable()) {
        ListView<?> found = findListView(c);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }
}
