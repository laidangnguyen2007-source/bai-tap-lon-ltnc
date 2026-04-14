package com.auction.server.service.impl;

import com.auction.server.dao.UserDao;
import com.auction.server.model.entity.user.Bidder;
import com.auction.server.model.entity.user.User;
import com.auction.server.model.exception.AuctionException;
// import com.auction.server.util.PasswordHasher;
import com.auction.server.service.UserService;
import java.util.Optional;

public class UserServiceImpl implements UserService {

  private final UserDao userDao;

  // constructor
  public UserServiceImpl(UserDao userDao) {
    this.userDao = userDao;
  }

  public void register(User user) throws AuctionException {
    // kiểm tra username tồn tại = boolean
    if (userDao.existsByUsername(user.getUsername())) {
      throw new AuctionException("username has been used");
    }
    // kiểm tra email tồn tại = boolean
    if (userDao.existsByEmail(user.getEmail())) {
      throw new AuctionException("email has been used");
    }

    user.setPasswordHash(user.getPasswordHash());

    userDao.save(user);
  }

  public User login(String username, String password) throws AuctionException {
    return userDao
        .findByUsername(username)
        .filter(user -> user.getPasswordHash().equals(password))
        .orElseThrow(() -> new AuctionException("invalid username or password"));
  }

  // Lấy thông tin user để sử dụng
  // Cần thêm thuộc tính Id và sửa thành findById
  public Optional<User> getUserById(String username) throws AuctionException {
    return userDao.findByUsername(username);
  }

  // sửa username -> int userId
  public void updateBalance(String username, long amount) throws AuctionException {
    User user =
        userDao.findByUsername(username).orElseThrow(() -> new AuctionException("user not found"));

    if (!(user instanceof Bidder)) {
      throw new AuctionException(
          "Operation failed: This account type does not support balance updates.");
    }

    if (user instanceof Bidder bidder) {
      try {
        if (amount >= 0) {
          bidder.setBalance(bidder.getBalance() + amount);
        } else {
          bidder.deductBalance(amount);
        }
      } catch (IllegalArgumentException e) {
        throw new AuctionException(e.getMessage());
      }
    } else {
      throw new AuctionException("This user type does not support balance operations.");
    }
  }

  // sửa username -> int userId
  public boolean isAuthorized(String username, String requiredRole) {
    User user =
        userDao
            .findByUsername(username)
            .orElseThrow(() -> new AuctionException("User not found: " + username));
    return user.getRole().name().equalsIgnoreCase(requiredRole);
  }
}
