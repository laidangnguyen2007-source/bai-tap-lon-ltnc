package server.model.exception;

// Được throw khi đăng nhập thất bại: sai username hoặc sai mật khẩu
// Tách riêng khỏi AuctionException để tuân thủ SRP — lỗi xác thực khác bản
// chất so với lỗi nghiệp vụ đấu giá, mỗi loại cần xử lý khác nhau ở Controller.
public class AuthenticationException extends AuctionException {
  public AuthenticationException(String message) {
    super(message);
  }
}
