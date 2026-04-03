// Interface DAO cho User, mở rộng GenericDao với các truy vấn đặc thù cho User
// Hỗ trợ tìm kiếm theo username, email và vai trò (role)
package com.auction.server.dao;

import com.auction.server.model.entity.user.User;
import com.auction.server.model.enums.UserRole;
import java.util.List;
import java.util.Optional;

public interface UserDao extends GenericDao<User> {

  // Tìm user theo username (dùng khi đăng nhập)
  Optional<User> findByUsername(String username);

  // Tìm user theo email (dùng khi đăng ký để kiểm tra trùng)
  Optional<User> findByEmail(String email);

  // Lấy tất cả user theo vai trò (VD: lấy hết Bidder, hoặc hết Seller)
  List<User> findByRole(UserRole role);

  // Kiểm tra username đã tồn tại chưa (dùng khi đăng ký)
  boolean existsByUsername(String username);

  // Kiểm tra email đã tồn tại chưa (dùng khi đăng ký)
  boolean existsByEmail(String email);
}
