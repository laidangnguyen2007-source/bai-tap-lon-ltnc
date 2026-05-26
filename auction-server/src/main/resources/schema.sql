-- Khởi tạo Database
CREATE DATABASE IF NOT EXISTS auction_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE auction_db;

-- 1. BẢNG USERS (Bidder, Seller, Admin)
CREATE TABLE IF NOT EXISTS users (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    username      VARCHAR(100)    NOT NULL,
    password_hash VARCHAR(255)    NOT NULL,
    email         VARCHAR(255)    NOT NULL,
    role          VARCHAR(20)     NOT NULL, -- BIDDER | SELLER | ADMIN
    balance       BIGINT          DEFAULT 0, -- Bidder
    shop_name     VARCHAR(255)    DEFAULT NULL, -- Seller
    rating        DOUBLE          DEFAULT 0.0, -- Seller
    access_level  INT             DEFAULT 1, -- Admin
    PRIMARY KEY (id),
    UNIQUE KEY uq_username (username),
    UNIQUE KEY uq_email (email),
    CONSTRAINT chk_role CHECK (role IN ('BIDDER', 'SELLER', 'ADMIN')),
    CONSTRAINT chk_access_level CHECK (access_level BETWEEN 1 AND 3),
    CONSTRAINT chk_balance CHECK (balance >= 0),
    CONSTRAINT chk_rating CHECK (rating BETWEEN 0.0 AND 5.0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. BẢNG ITEMS (Electronics, Artwork, Vehicle, Other)
CREATE TABLE IF NOT EXISTS items (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    name            VARCHAR(255)    NOT NULL,
    description     TEXT            DEFAULT NULL,
    image_base64    LONGTEXT        DEFAULT NULL,
    item_specifics  TEXT            DEFAULT NULL,
    starting_price  BIGINT          NOT NULL,
    seller_id       BIGINT          NOT NULL,
    category        VARCHAR(50)     NOT NULL, -- ELECTRONICS | ARTWORK | VEHICLE | OTHER
    brand           VARCHAR(100)    DEFAULT NULL, -- Electronics
    warranty_months INT             DEFAULT 0, -- Electronics
    power_watts     DOUBLE          DEFAULT 0.0, -- Electronics
    artist          VARCHAR(255)    DEFAULT NULL, -- Artwork
    art_year        INT             DEFAULT NULL, -- Artwork
    medium          VARCHAR(100)    DEFAULT NULL, -- Artwork
    make            VARCHAR(100)    DEFAULT NULL, -- Vehicle
    model           VARCHAR(100)    DEFAULT NULL, -- Vehicle
    vehicle_year    INT             DEFAULT NULL, -- Vehicle
    mileage         DOUBLE          DEFAULT 0.0, -- Vehicle
    PRIMARY KEY (id),
    CONSTRAINT fk_item_seller FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_category CHECK (category IN ('ELECTRONICS', 'ARTWORK', 'VEHICLE', 'OTHER')),
    CONSTRAINT chk_starting_price CHECK (starting_price > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. BẢNG AUCTIONS
CREATE TABLE IF NOT EXISTS auctions (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    created_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    item_id             BIGINT      NOT NULL,
    seller_id           BIGINT      NOT NULL,
    current_price       BIGINT      NOT NULL DEFAULT 0,
    current_winner_id   BIGINT      DEFAULT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'OPEN', -- OPEN | RUNNING | FINISHED | PAID | CANCELED
    start_time          DATETIME    NOT NULL,
    end_time            DATETIME    NOT NULL,
    min_bid_step        BIGINT      NOT NULL DEFAULT 0,
    settled             BOOLEAN     NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_auction_item FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_auction_seller FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_auction_winner FOREIGN KEY (current_winner_id) REFERENCES users(id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT chk_auction_status CHECK (status IN ('OPEN', 'RUNNING', 'FINISHED', 'PAID', 'CANCELED')),
    CONSTRAINT chk_auction_time CHECK (end_time > start_time),
    CONSTRAINT chk_current_price CHECK (current_price >= 0),
    INDEX idx_auction_status (status),
    INDEX idx_auction_seller (seller_id),
    INDEX idx_auction_item (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. BẢNG BID_TRANSACTIONS
CREATE TABLE IF NOT EXISTS bid_transactions (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    auction_id  BIGINT      NOT NULL,
    bidder_id   BIGINT      NOT NULL,
    amount      BIGINT      NOT NULL,
    timestamp   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_bid_auction FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_bid_bidder FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_bid_amount CHECK (amount > 0),
    INDEX idx_bid_auction (auction_id),
    INDEX idx_bid_bidder (bidder_id),
    INDEX idx_bid_timestamp (auction_id, timestamp ASC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. BẢNG WALLETS
CREATE TABLE IF NOT EXISTS wallets (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    user_id           BIGINT       NOT NULL,
    total_balance     BIGINT       NOT NULL DEFAULT 0,
    available_balance BIGINT       NOT NULL DEFAULT 0,
    locked_balance    BIGINT       NOT NULL DEFAULT 0,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_wallet_user (user_id),
    CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_available_balance CHECK (available_balance >= 0),
    CONSTRAINT chk_locked_balance CHECK (locked_balance >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. BẢNG WALLET_TRANSACTIONS
CREATE TABLE IF NOT EXISTS wallet_transactions (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    user_id          BIGINT       NOT NULL,
    type             VARCHAR(30)  NOT NULL,
    amount           BIGINT       NOT NULL,
    balance_before   BIGINT       NOT NULL,
    balance_after    BIGINT       NOT NULL,
    reference_id     BIGINT       DEFAULT NULL,
    reference_type   VARCHAR(50)  DEFAULT NULL,
    description      TEXT         DEFAULT NULL,
    created_by       VARCHAR(50)  DEFAULT NULL,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_wt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_wt_user (user_id),
    INDEX idx_wt_type (type),
    INDEX idx_wt_ref (reference_type, reference_id),
    INDEX idx_wt_created (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. BẢNG AUTO_BIDS
CREATE TABLE IF NOT EXISTS auto_bids (
    id           BIGINT    NOT NULL AUTO_INCREMENT,
    auction_id   BIGINT    NOT NULL,
    bidder_id    BIGINT    NOT NULL,
    max_bid      BIGINT    NOT NULL,
    increment    BIGINT    NOT NULL,
    is_active    BOOLEAN   NOT NULL DEFAULT TRUE,
    created_at   DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_ab_auction FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_ab_bidder FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    UNIQUE KEY uq_autobid (auction_id, bidder_id),
    INDEX idx_ab_auction (auction_id),
    INDEX idx_ab_bidder (bidder_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ================================================================
-- DỮ LIỆU MẪU (SEED DATA)
-- ================================================================

INSERT IGNORE INTO users (id, username, password_hash, email, role, balance, shop_name, rating, access_level) VALUES
    (1, 'bidder1', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'bidder1@test.com', 'BIDDER', 10000000, NULL, 0.0, 1),
    (2, 'bidder2', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'bidder2@test.com', 'BIDDER', 5000000, NULL, 0.0, 1),
    (3, 'seller1', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'seller1@test.com', 'SELLER', 0, 'Shop ABC', 4.5, 1),
    (4, 'seller2', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'seller2@test.com', 'SELLER', 0, 'Shop XYZ', 4.0, 1),
    (5, 'admin', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'admin@test.com', 'ADMIN', 0, NULL, 0.0, 3);

INSERT IGNORE INTO items (id, name, description, starting_price, seller_id, category, brand, warranty_months, power_watts, artist, art_year, medium, make, vehicle_year, mileage) VALUES
    (1, 'Laptop Dell XPS 15', 'Laptop gaming cao cấp, CPU Intel i9, RAM 32GB, SSD 1TB', 15000000, 3, 'ELECTRONICS', 'Dell', 24, 135.0, NULL, NULL, NULL, NULL, NULL, 0.0),
    (2, 'iPhone 15 Pro Max', 'Điện thoại Apple mới nhất, 256GB, màu titan tự nhiên', 25000000, 3, 'ELECTRONICS', 'Apple', 12, 25.0, NULL, NULL, NULL, NULL, NULL, 0.0),
    (3, 'Bức tranh Đồng Quê', 'Tranh sơn dầu vẽ tay, phong cảnh làng quê Việt Nam', 3000000, 4, 'ARTWORK', NULL, 0, 0.0, 'Nguyễn Văn Hoà', 2020, 'Sơn dầu', NULL, NULL, 0.0),
    (4, 'Honda Wave Alpha 2022', 'Xe máy đi ít, còn mới 95%, đầy đủ giấy tờ', 8000000, 4, 'VEHICLE', NULL, 0, 0.0, NULL, NULL, NULL, 'Honda', 2022, 3500.0);

INSERT IGNORE INTO auctions (id, item_id, seller_id, current_price, status, start_time, end_time) VALUES
    (1, 1, 3, 15000000, 'RUNNING', DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 2 HOUR)),
    (2, 2, 3, 25000000, 'OPEN', DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 3 DAY)),
    (3, 3, 4, 4500000, 'FINISHED', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 HOUR)),
    (4, 4, 4, 8000000, 'RUNNING', DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_ADD(NOW(), INTERVAL 90 MINUTE));

INSERT IGNORE INTO bid_transactions (id, auction_id, bidder_id, amount, timestamp) VALUES
    (1, 1, 1, 15500000, DATE_SUB(NOW(), INTERVAL 50 MINUTE)),
    (2, 1, 2, 16000000, DATE_SUB(NOW(), INTERVAL 45 MINUTE)),
    (3, 1, 1, 16500000, DATE_SUB(NOW(), INTERVAL 40 MINUTE)),
    (4, 1, 2, 17000000, DATE_SUB(NOW(), INTERVAL 35 MINUTE)),
    (5, 1, 1, 17500000, DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
    (6, 1, 2, 18000000, DATE_SUB(NOW(), INTERVAL 10 MINUTE)),
    (7, 1, 1, 18500000, DATE_SUB(NOW(), INTERVAL 5 MINUTE));

UPDATE auctions SET current_price = 18500000, current_winner_id = 1 WHERE id = 1;

INSERT IGNORE INTO wallets (id, user_id, total_balance, available_balance, locked_balance) VALUES
    (1, 1, 10000000, 10000000, 0),
    (2, 2, 5000000,  5000000,  0),
    (3, 3, 0,        0,        0),
    (4, 4, 0,        0,        0),
    (5, 5, 0,        0,        0);