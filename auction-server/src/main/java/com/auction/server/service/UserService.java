// Service xử lý nghiệp vụ liên quan đến User: đăng ký, đăng nhập, quản lý tài khoản
// Tầng Service nằm giữa Controller và DAO, chứa business logic
// Controller gọi Service → Service gọi DAO → DAO truy cập dữ liệu
package com.auction.server.service;

import com.auction.server.dao.UserDao;
import com.auction.server.model.entity.user.Admin;
import com.auction.server.model.entity.user.Bidder;
import com.auction.server.model.entity.user.Seller;
import com.auction.server.model.entity.user.User;
import com.auction.server.model.enums.UserRole;
import com.auction.server.model.exception.AuctionException;
import com.auction.server.service.util.PasswordUtil;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class UserService {

  private final UserDao userDao;

  public UserService(UserDao userDao) {
    this.userDao = Objects.requireNonNull(userDao, "userDao must not be null");
  }

  // ===== ĐĂNG KÝ TÀI KHOẢN =====

  // Đăng ký tài khoản Bidder (người đấu giá)
  public User registerBidder(String username, String rawPassword, String email, double balance) {
    validateRegistration(username, rawPassword, email);
    String passwordHash = PasswordUtil.hashPassword(rawPassword);
    Bidder bidder = new Bidder(username, passwordHash, email, balance);
    return userDao.save(bidder);
  }

  // Đăng ký tài khoản Seller (người bán)
  public User registerSeller(String username, String rawPassword, String email, String shopName) {
    validateRegistration(username, rawPassword, email);
    String passwordHash = PasswordUtil.hashPassword(rawPassword);
    Seller seller = new Seller(username, passwordHash, email, shopName);
    return userDao.save(seller);
  }

  // Đăng ký tài khoản Admin (quản trị viên)
  public User registerAdmin(String username, String rawPassword, String email) {
    validateRegistration(username, rawPassword, email);
    String passwordHash = PasswordUtil.hashPassword(rawPassword);
    Admin admin = new Admin(username, passwordHash, email);
    return userDao.save(admin);
  }

  // ===== ĐĂNG NHẬP =====

  // Đăng nhập: kiểm tra username + password, trả về User nếu hợp lệ
  public User login(String username, String rawPassword) {
    Objects.requireNonNull(username, "Username must not be null");
    Objects.requireNonNull(rawPassword, "Password must not be null");

    // Tìm user theo username
    User user =
        userDao
            .findByUsername(username)
            .orElseThrow(() -> new AuctionException("Username not found: " + username));

    // So sánh hash mật khẩu nhập vào với hash đã lưu
    if (!PasswordUtil.verifyPassword(rawPassword, user.getPasswordHash())) {
      throw new AuctionException("Incorrect password");
    }

    return user;
  }

  // ===== TRUY VẤN =====

  // Tìm user theo ID
  public Optional<User> findById(Long id) {
    return userDao.findById(id);
  }

  // Tìm user theo username
  public Optional<User> findByUsername(String username) {
    return userDao.findByUsername(username);
  }

  // Lấy tất cả user theo vai trò
  public List<User> findByRole(UserRole role) {
    return userDao.findByRole(role);
  }

  // Lấy tất cả user trong hệ thống
  public List<User> findAll() {
    return userDao.findAll();
  }

  // ===== VALIDATION =====

  // Kiểm tra dữ liệu đầu vào khi đăng ký
  private void validateRegistration(String username, String rawPassword, String email) {
    Objects.requireNonNull(username, "Username must not be null");
    Objects.requireNonNull(rawPassword, "Password must not be null");
    Objects.requireNonNull(email, "Email must not be null");

    if (username.isBlank()) {
      throw new AuctionException("Username must not be blank");
    }
    if (rawPassword.length() < 6) {
      throw new AuctionException("Password must be at least 6 characters");
    }
    if (!email.contains("@")) {
      throw new AuctionException("Invalid email format");
    }

    // Kiểm tra trùng lặp username và email
    if (userDao.existsByUsername(username)) {
      throw new AuctionException("Username already exists: " + username);
    }
    if (userDao.existsByEmail(email)) {
      throw new AuctionException("Email already registered: " + email);
    }
  }
}
