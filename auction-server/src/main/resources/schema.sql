-- ================================================================
-- AUCTION SYSTEM - Database Schema (H2 Compatible SQL)
-- Chạy file này lần đầu để tạo cấu trúc bảng.
-- H2 sẽ tự động chạy file này khi khởi động Server (init script).
-- ================================================================

-- Bảng người dùng (User)
-- Dùng chung 1 bảng cho Bidder, Seller, Admin (Single Table Inheritance)
-- Cột ROLE phân biệt loại User
CREATE TABLE IF NOT EXISTS users (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  username      VARCHAR(100)  NOT NULL UNIQUE,
  password_hash VARCHAR(255)  NOT NULL,
  email         VARCHAR(255)  NOT NULL UNIQUE,
  role          VARCHAR(20)   NOT NULL, -- BIDDER, SELLER, ADMIN
  -- Dành riêng cho Bidder
  balance       DOUBLE        DEFAULT 0.0,
  -- Dành riêng cho Seller
  shop_name     VARCHAR(255),
  rating        DOUBLE        DEFAULT 0.0,
  -- Dành riêng cho Admin
  access_level  INT           DEFAULT 1
) ENGINE=InnoDB;

-- Bảng sản phẩm (Item)
-- Dùng Single Table Inheritance tương tự User
CREATE TABLE IF NOT EXISTS items (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  name            VARCHAR(255)  NOT NULL,
  description     TEXT,
  starting_price  DOUBLE        NOT NULL,
  seller_id       BIGINT        NOT NULL,
  category        VARCHAR(20)   NOT NULL, -- ELECTRONICS, ARTWORK, VEHICLE
  -- Dành riêng cho Electronics
  brand           VARCHAR(100),
  warranty_months INT           DEFAULT 0,
  power_watts     DOUBLE        DEFAULT 0.0,
  -- Dành riêng cho Artwork
  artist          VARCHAR(255),
  art_year        INT,
  medium          VARCHAR(100),
  -- Dành riêng cho Vehicle
  make            VARCHAR(100),
  model           VARCHAR(100),
  vehicle_year    INT,
  mileage         DOUBLE        DEFAULT 0.0,
  FOREIGN KEY (seller_id) REFERENCES users(id)
) ENGINE=InnoDB;

-- Bảng phiên đấu giá (Auction)
CREATE TABLE IF NOT EXISTS auctions (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  created_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  item_id           BIGINT      NOT NULL,
  seller_id         BIGINT      NOT NULL,
  current_price     DOUBLE      NOT NULL DEFAULT 0.0,
  current_winner_id BIGINT,  -- NULL nếu chưa có ai đặt giá
  status            VARCHAR(20) NOT NULL DEFAULT 'OPEN', -- OPEN, RUNNING, FINISHED, PAID, CANCELED
  start_time        TIMESTAMP   NOT NULL,
  end_time          TIMESTAMP   NOT NULL,
  FOREIGN KEY (item_id)           REFERENCES items(id),
  FOREIGN KEY (seller_id)         REFERENCES users(id),
  FOREIGN KEY (current_winner_id) REFERENCES users(id)
) ENGINE=InnoDB;

-- Bảng lịch sử đặt giá (BidTransaction)
CREATE TABLE IF NOT EXISTS bid_transactions (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  auction_id  BIGINT    NOT NULL,
  bidder_id   BIGINT    NOT NULL,
  amount      DOUBLE    NOT NULL,
  timestamp   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (auction_id) REFERENCES auctions(id),
  FOREIGN KEY (bidder_id)  REFERENCES users(id)
) ENGINE=InnoDB;
