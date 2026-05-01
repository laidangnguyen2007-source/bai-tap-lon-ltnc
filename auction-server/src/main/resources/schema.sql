-- Script khởi tạo Database cho Hệ thống Đấu giá Online
CREATE DATABASE IF NOT EXISTS auction_db;
USE auction_db;

-- 1. Bảng người dùng (Users)
CREATE TABLE IF NOT EXISTS users (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    username      VARCHAR(100)    NOT NULL,
    password_hash VARCHAR(255)    NOT NULL,
    email         VARCHAR(255)    NOT NULL,
    role          VARCHAR(20)     NOT NULL,
    balance       BIGINT          DEFAULT 0,
    shop_name     VARCHAR(255)    DEFAULT NULL,
    rating        DOUBLE          DEFAULT 0.0,
    access_level  INT             DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uq_username (username),
    UNIQUE KEY uq_email    (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Bảng sản phẩm (Items)
CREATE TABLE IF NOT EXISTS items (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    name            VARCHAR(255)    NOT NULL,
    description     TEXT            DEFAULT NULL,
    starting_price  BIGINT          NOT NULL,
    seller_id       BIGINT          NOT NULL,
    category        VARCHAR(20)     NOT NULL,
    brand           VARCHAR(100),
    warranty_months INT,
    power_watts     DOUBLE,
    artist          VARCHAR(100),
    art_year        INT,
    medium          VARCHAR(100),
    make            VARCHAR(100),
    model           VARCHAR(100),
    vehicle_year    INT,
    mileage         DOUBLE,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Bảng phiên đấu giá (Auctions)
CREATE TABLE IF NOT EXISTS auctions (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    created_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    item_id             BIGINT      NOT NULL,
    seller_id           BIGINT      NOT NULL,
    current_price       BIGINT      NOT NULL DEFAULT 0,
    current_winner_id   BIGINT      DEFAULT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'RUNNING',
    start_time          DATETIME    NOT NULL,
    end_time            DATETIME    NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Bảng giao dịch đặt giá (BidTransactions)
CREATE TABLE IF NOT EXISTS bid_transactions (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    created_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    auction_id          BIGINT      NOT NULL,
    bidder_id           BIGINT      NOT NULL,
    amount              BIGINT      NOT NULL,
    timestamp           DATETIME    NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. Nạp dữ liệu mẫu (Seed Data)
-- Tài khoản mặc định (Mật khẩu: 123456)
INSERT IGNORE INTO users (username, password_hash, email, role, balance)
VALUES ('bidder1', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'bidder1@test.com', 'BIDDER', 10000000),
       ('seller1', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'seller1@test.com', 'SELLER', 0);

-- Sản phẩm và phiên đấu giá mẫu
INSERT IGNORE INTO items (id, name, description, starting_price, seller_id, category, brand) 
VALUES (1, 'Laptop ASUS ROG', 'Gaming Laptop i7', 20000000, 2, 'ELECTRONICS', 'ASUS'),
       (2, 'Mechanical Keyboard', 'RGB Blue Switch', 1500000, 2, 'ELECTRONICS', 'Custom');

INSERT IGNORE INTO auctions (id, item_id, seller_id, current_price, status, start_time, end_time)
VALUES (1, 1, 2, 21000000, 'RUNNING', NOW(), DATE_ADD(NOW(), INTERVAL 1 DAY)),
       (2, 2, 2, 1600000, 'RUNNING', NOW(), DATE_ADD(NOW(), INTERVAL 1 DAY));