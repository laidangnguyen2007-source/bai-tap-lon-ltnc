package com.auction.server.service;

import com.auction.server.model.entity.user.User;
import com.auction.server.model.exception.AuctionException;
import java.util.Optional;

public interface UserService {

  // Đăng kí tài khoản người dùng mới
  public void register(User user) throws Exception;

  /*
   * Xác thực thông tin đăng nhập Trả về object User tương ứng nếu thành công AuctionException nếu
   * sai thông tin đăng nhập
   */
  public User login(String username, String password) throws AuctionException;

  // sửa lại username -> int userId
  public Optional<User> getUserById(String username) throws AuctionException;

  // Cập nhật số dư tài khoản
  // Sửa username -> int userId
  public void updateBalance(String username, long amount) throws AuctionException;

  // Kiểm tra người dùng có quyền hạn thực hiện hành động không
  // sửa username -> int userId
  public boolean isAuthorized(String username, String requiredRole) throws AuctionException;
}
