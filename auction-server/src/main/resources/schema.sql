-- ================================================================
-- AUCTION SYSTEM — MySQL Database Setup
-- Chạy file này 1 lần duy nhất để khởi tạo toàn bộ database.
-- Yêu cầu: MySQL Server đang chạy ở port 3306, user root, password rỗng.
--
-- Cách chạy:
--   mysql -u root < auction_db_setup.sql
-- Hoặc mở MySQL Workbench / phpMyAdmin rồi paste toàn bộ nội dung vào.
-- ================================================================


-- ----------------------------------------------------------------
-- 1. TẠO VÀ CHỌN DATABASE
-- ----------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS auction_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE auction_db;


-- ----------------------------------------------------------------
-- 2. BẢNG USERS  (Single Table Inheritance)
--    Gộp Bidder / Seller / Admin vào 1 bảng.
--    Cột ROLE phân biệt loại — các cột còn lại NULL nếu không dùng.
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    username      VARCHAR(100)    NOT NULL,
    password_hash VARCHAR(255)    NOT NULL,
    email         VARCHAR(255)    NOT NULL,
    role          VARCHAR(20)     NOT NULL COMMENT 'BIDDER | SELLER | ADMIN',

    -- Chỉ Bidder dùng
    balance       BIGINT          DEFAULT 0,

    -- Chỉ Seller dùng
    shop_name     VARCHAR(255)    DEFAULT NULL,
    rating        DOUBLE          DEFAULT 0.0,

    -- Chỉ Admin dùng
    access_level  INT             DEFAULT 1,

    PRIMARY KEY (id),
    UNIQUE KEY uq_username (username),
    UNIQUE KEY uq_email    (email),

    CONSTRAINT chk_role
        CHECK (role IN ('BIDDER', 'SELLER', 'ADMIN')),
    CONSTRAINT chk_access_level
        CHECK (access_level BETWEEN 1 AND 3),
    CONSTRAINT chk_balance
        CHECK (balance >= 0),
    CONSTRAINT chk_rating
        CHECK (rating BETWEEN 0.0 AND 5.0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ----------------------------------------------------------------
-- 3. BẢNG ITEMS  (Single Table Inheritance)
--    Gộp Electronics / Artwork / Vehicle vào 1 bảng.
--    Cột CATEGORY phân biệt loại.
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS items (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    name            VARCHAR(255)    NOT NULL,
    description     TEXT            DEFAULT NULL,
    image_base64    LONGTEXT        DEFAULT NULL,
    starting_price  BIGINT          NOT NULL,
    seller_id       BIGINT          NOT NULL,
    category        VARCHAR(20)     NOT NULL COMMENT 'ELECTRONICS | ARTWORK | VEHICLE',

    -- Chỉ Electronics dùng
    brand           VARCHAR(100)    DEFAULT NULL,
    warranty_months INT             DEFAULT 0,
    power_watts     DOUBLE          DEFAULT 0.0,

    -- Chỉ Artwork dùng
    artist          VARCHAR(255)    DEFAULT NULL,
    art_year        INT             DEFAULT NULL,
    medium          VARCHAR(100)    DEFAULT NULL,

    -- Chỉ Vehicle dùng
    make            VARCHAR(100)    DEFAULT NULL,
    model           VARCHAR(100)    DEFAULT NULL,
    vehicle_year    INT             DEFAULT NULL,
    mileage         DOUBLE          DEFAULT 0.0,

    PRIMARY KEY (id),
    CONSTRAINT fk_item_seller
        FOREIGN KEY (seller_id) REFERENCES users(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_category
        CHECK (category IN ('ELECTRONICS', 'ARTWORK', 'VEHICLE', 'OTHER')),
    CONSTRAINT chk_starting_price
        CHECK (starting_price > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ----------------------------------------------------------------
-- 4. BẢNG AUCTIONS
--    Mỗi phiên đấu giá gắn với 1 item và 1 seller.
--    current_winner_id NULL = chưa có ai đặt giá.
--    ENGINE=InnoDB bắt buộc để dùng SELECT ... FOR UPDATE (Pessimistic Lock).
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS auctions (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    created_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    item_id             BIGINT      NOT NULL,
    seller_id           BIGINT      NOT NULL,
    current_price       BIGINT      NOT NULL DEFAULT 0,
    current_winner_id   BIGINT      DEFAULT NULL COMMENT 'NULL nếu chưa có ai đặt giá',
    status              VARCHAR(20) NOT NULL DEFAULT 'OPEN'
                            COMMENT 'OPEN | RUNNING | FINISHED | PAID | CANCELED',
    start_time          DATETIME    NOT NULL,
    end_time            DATETIME    NOT NULL,
    min_bid_step        BIGINT      NOT NULL DEFAULT 0 COMMENT 'Số tiền tối thiểu phải cộng thêm mỗi lượt',

    PRIMARY KEY (id),
    CONSTRAINT fk_auction_item
        FOREIGN KEY (item_id)           REFERENCES items(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_auction_seller
        FOREIGN KEY (seller_id)         REFERENCES users(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_auction_winner
        FOREIGN KEY (current_winner_id) REFERENCES users(id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT chk_auction_status
        CHECK (status IN ('OPEN', 'RUNNING', 'FINISHED', 'PAID', 'CANCELED')),
    CONSTRAINT chk_auction_time
        CHECK (end_time > start_time),
    CONSTRAINT chk_current_price
        CHECK (current_price >= 0),

    -- Index để tăng tốc query lọc theo status (dùng nhiều trong AuctionListController)
    INDEX idx_auction_status   (status),
    INDEX idx_auction_seller   (seller_id),
    INDEX idx_auction_item     (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ----------------------------------------------------------------
-- 5. BẢNG BID_TRANSACTIONS
--    Bất biến (Immutable) — không bao giờ UPDATE, chỉ INSERT + SELECT.
--    Dùng để vẽ LineChart realtime trong BiddingRoomController.
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS bid_transactions (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    auction_id  BIGINT      NOT NULL,
    bidder_id   BIGINT      NOT NULL,
    amount      BIGINT      NOT NULL,
    timestamp   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_bid_auction
        FOREIGN KEY (auction_id) REFERENCES auctions(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_bid_bidder
        FOREIGN KEY (bidder_id)  REFERENCES users(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_bid_amount
        CHECK (amount > 0),

    -- Index tăng tốc query lấy lịch sử bid theo phiên (dùng trong BidController)
    INDEX idx_bid_auction   (auction_id),
    INDEX idx_bid_bidder    (bidder_id),
    INDEX idx_bid_timestamp (auction_id, timestamp ASC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ================================================================
-- 6. DỮ LIỆU MẪU (SEED DATA) — để test ngay sau khi chạy
-- ================================================================

-- Mật khẩu của tất cả tài khoản mẫu đều là: "123456"
-- SHA-256("123456") = 8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92

INSERT IGNORE INTO users (username, password_hash, email, role, balance, shop_name, rating, access_level)
VALUES
    -- Bidder
    ('bidder1',  '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92',
     'bidder1@test.com',  'BIDDER',  10000000, NULL,         0.0, 1),
    ('bidder2',  '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92',
     'bidder2@test.com',  'BIDDER',  5000000,  NULL,         0.0, 1),

    -- Seller
    ('seller1',  '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92',
     'seller1@test.com',  'SELLER',  0,        'Shop ABC',   4.5, 1),
    ('seller2',  '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92',
     'seller2@test.com',  'SELLER',  0,        'Shop XYZ',   4.0, 1),

    -- Admin
    ('admin',    '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92',
     'admin@test.com',    'ADMIN',   0,        NULL,         0.0, 3);


-- Sản phẩm mẫu (seller1 = id 3, seller2 = id 4)
INSERT IGNORE INTO items (name, description, starting_price, seller_id, category,
                   brand, warranty_months, power_watts,
                   artist, art_year, medium,
                   make, vehicle_year, mileage)
VALUES
    -- Electronics
    ('Laptop Dell XPS 15',
     'Laptop gaming cao cấp, CPU Intel i9, RAM 32GB, SSD 1TB',
     15000000, 3, 'ELECTRONICS',
     'Dell', 24, 135.0,
     NULL, NULL, NULL,
     NULL, NULL, 0.0),

    ('iPhone 15 Pro Max',
     'Điện thoại Apple mới nhất, 256GB, màu titan tự nhiên',
     25000000, 3, 'ELECTRONICS',
     'Apple', 12, 25.0,
     NULL, NULL, NULL,
     NULL, NULL, 0.0),

    -- Artwork
    ('Bức tranh Đồng Quê',
     'Tranh sơn dầu vẽ tay, phong cảnh làng quê Việt Nam',
     3000000, 4, 'ARTWORK',
     NULL, 0, 0.0,
     'Nguyễn Văn Hoà', 2020, 'Sơn dầu',
     NULL, NULL, 0.0),

    -- Vehicle
    ('Honda Wave Alpha 2022',
     'Xe máy đi ít, còn mới 95%, đầy đủ giấy tờ',
     8000000, 4, 'VEHICLE',
     NULL, 0, 0.0,
     NULL, NULL, NULL,
     'Honda', 2022, 3500.0);


-- Phiên đấu giá mẫu
INSERT IGNORE INTO auctions (item_id, seller_id, current_price, status, start_time, end_time)
VALUES
    -- Phiên đang RUNNING (Laptop Dell)
    (1, 3, 15000000, 'RUNNING',
     DATE_SUB(NOW(), INTERVAL 1 HOUR),
     DATE_ADD(NOW(),  INTERVAL 2 HOUR)),

    -- Phiên đang OPEN (iPhone)
    (2, 3, 25000000, 'OPEN',
     DATE_ADD(NOW(), INTERVAL 1 DAY),
     DATE_ADD(NOW(), INTERVAL 3 DAY)),

    -- Phiên đã FINISHED (Tranh)
    (3, 4, 4500000,  'FINISHED',
     DATE_SUB(NOW(), INTERVAL 2 DAY),
     DATE_SUB(NOW(), INTERVAL 1 HOUR)),

    -- Phiên đang RUNNING (Honda Wave)
    (4, 4, 8000000,  'RUNNING',
     DATE_SUB(NOW(), INTERVAL 30 MINUTE),
     DATE_ADD(NOW(), INTERVAL 90 MINUTE));


-- Lịch sử bid mẫu cho phiên 1 (Laptop Dell) — dùng để vẽ LineChart
-- bidder1 = id 1, bidder2 = id 2
INSERT IGNORE INTO bid_transactions (auction_id, bidder_id, amount, timestamp)
VALUES
    (1, 1, 15500000, DATE_SUB(NOW(), INTERVAL 50 MINUTE)),
    (1, 2, 16000000, DATE_SUB(NOW(), INTERVAL 45 MINUTE)),
    (1, 1, 16500000, DATE_SUB(NOW(), INTERVAL 40 MINUTE)),
    (1, 2, 17000000, DATE_SUB(NOW(), INTERVAL 35 MINUTE)),
    (1, 1, 17500000, DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
    (1, 2, 18000000, DATE_SUB(NOW(), INTERVAL 10 MINUTE)),
    (1, 1, 18500000, DATE_SUB(NOW(), INTERVAL  5 MINUTE));

-- Cập nhật giá hiện tại và người dẫn đầu của phiên 1
UPDATE auctions
SET current_price = 18500000, current_winner_id = 1
WHERE id = 1;


-- ----------------------------------------------------------------
-- 6. BẢNG WALLETS
--    Ví tiền cho tất cả user (Bidder, Seller, Admin).
--    total_balance = available_balance + locked_balance (luôn đúng).
--    available_balance: tiền có thể dùng ngay.
--    locked_balance: tiền đang bị giữ cho các phiên đấu giá đang diễn ra.
-- ----------------------------------------------------------------
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
    CONSTRAINT fk_wallet_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_available_balance CHECK (available_balance >= 0),
    CONSTRAINT chk_locked_balance    CHECK (locked_balance >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ----------------------------------------------------------------
-- 7. BẢNG WALLET_TRANSACTIONS
--    Bất biến (Immutable) — chỉ INSERT + SELECT.
--    Ghi lại mọi thay đổi số dư ví: lock, release, settlement, admin adjust...
--    reference_id + reference_type: liên kết đến auction/bid liên quan.
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wallet_transactions (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    user_id          BIGINT       NOT NULL,
    type             VARCHAR(30)  NOT NULL
                         COMMENT 'DEPOSIT|WITHDRAW|BID_LOCK|BID_RELEASE|AUCTION_WIN|SELLER_PAYOUT|AUTO_BID_LOCK|AUTO_BID_RELEASE|ADMIN_ADJUSTMENT|REFUND',
    amount           BIGINT       NOT NULL,
    balance_before   BIGINT       NOT NULL,
    balance_after    BIGINT       NOT NULL,
    reference_id     BIGINT       DEFAULT NULL  COMMENT 'ID phiên đấu giá hoặc bid liên quan',
    reference_type   VARCHAR(50)  DEFAULT NULL  COMMENT 'AUCTION, BID, ADMIN...',
    description      TEXT         DEFAULT NULL,
    created_by       VARCHAR(50)  DEFAULT NULL  COMMENT 'Actor thực hiện (USER, SYSTEM, ADMIN)',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_wt_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    INDEX idx_wt_user     (user_id),
    INDEX idx_wt_type     (type),
    INDEX idx_wt_ref      (reference_type, reference_id),
    INDEX idx_wt_created  (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ----------------------------------------------------------------
-- 8. BẢNG AUTO_BIDS
--    Lưu trữ các chiến lược auto-bid (persist qua restart).
--    Mỗi user chỉ có 1 auto-bid active cho mỗi auction.
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS auto_bids (
    id           BIGINT    NOT NULL AUTO_INCREMENT,
    auction_id   BIGINT    NOT NULL,
    bidder_id    BIGINT    NOT NULL,
    max_bid      BIGINT    NOT NULL,
    increment    BIGINT    NOT NULL,
    is_active    BOOLEAN   NOT NULL DEFAULT TRUE,
    created_at   DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_ab_auction
        FOREIGN KEY (auction_id) REFERENCES auctions(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_ab_bidder
        FOREIGN KEY (bidder_id) REFERENCES users(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    UNIQUE KEY uq_autobid (auction_id, bidder_id),

    INDEX idx_ab_auction (auction_id),
    INDEX idx_ab_bidder  (bidder_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ================================================================
-- 9. SEED WALLETS (Tạo ví cho user mẫu)
-- ================================================================
INSERT IGNORE INTO wallets (user_id, total_balance, available_balance, locked_balance)
VALUES
    (1, 10000000, 10000000, 0),   -- bidder1: 10 triệu
    (2, 5000000,  5000000,  0),   -- bidder2: 5 triệu
    (3, 0,        0,        0),   -- seller1
    (4, 0,        0,        0),   -- seller2
    (5, 0,        0,        0);   -- admin


-- ================================================================
-- 10. AUTO-MIGRATION (Cập nhật cho các bản cũ)
-- Cần dùng thủ thuật hoặc chạy lệnh đơn lẻ vì MySQL không có 'ADD COLUMN IF NOT EXISTS' 
-- bản cũ. Các câu lệnh dưới đây đảm bảo máy cũ khi pull code về vẫn chạy được.
-- ================================================================

-- Thêm cột mô tả sản phẩm và ảnh Base64 nếu chưa có (cho bảng items)
-- Lưu ý: Nếu máy đã có rồi, các lệnh này có thể báo lỗi nhẹ trong log nhưng không làm dừng Server.
-- (Server của chúng ta được thiết kế để bỏ qua lỗi nhỏ khi khởi tạo schema).
-- ----------------------------------------------------------------
ALTER TABLE items ADD COLUMN image_base64 LONGTEXT DEFAULT NULL AFTER description;

-- Thêm cột bước giá tối thiểu cho bảng auctions nếu chưa có
ALTER TABLE auctions ADD COLUMN min_bid_step BIGINT NOT NULL DEFAULT 0 COMMENT 'Số tiền tối thiểu phải cộng thêm mỗi lượt' AFTER end_time;

-- Thêm cột settled (đánh dấu đã thanh toán) cho bảng auctions — idempotency protection
ALTER TABLE auctions ADD COLUMN settled BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'TRUE nếu đã settlement xong (chống duplicate)';

-- Đảm bảo category có hỗ trợ loại 'OTHER'
ALTER TABLE items MODIFY COLUMN category VARCHAR(50) NOT NULL;

-- Sửa cột created_by trong wallet_transactions thành VARCHAR(50) để lưu Enum TransactionActor (USER, ADMIN, SYSTEM)
ALTER TABLE wallet_transactions MODIFY COLUMN created_by VARCHAR(50) DEFAULT NULL COMMENT 'Actor thực hiện (USER, SYSTEM, ADMIN)';

SELECT 'Migration completed successfully' AS status;