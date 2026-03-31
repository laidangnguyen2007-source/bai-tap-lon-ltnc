package com.auction.server.service;

import com.auction.server.model.entity.user.User;
import java.util.Optional;

public interface UserService {

  User register(User user); // throws AuctionException;

  User login(String username, String password); // throws AuctionException;

  boolean hasRole(long userId, String requiredRole);

  Optional<User> findById(long userId);

}
