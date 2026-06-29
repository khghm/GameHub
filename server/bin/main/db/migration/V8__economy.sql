-- ============================================================
-- V8__economy.sql
-- جداول پایه اقتصاد: کیف پول، فروشگاه، موجودی، تراکنش‌ها، هدیه
-- (بدون Sink خودکار، بدون کسر مستقیم سکه)
-- ============================================================

-- ============================================================
-- اضافه کردن فیلدهای سکه و نسخه کیف پول به جدول users
-- ============================================================
ALTER TABLE users ADD COLUMN IF NOT EXISTS soft_currency BIGINT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS wallet_version BIGINT NOT NULL DEFAULT 1;

-- ============================================================
-- جدول تعریف آیتم‌ها (item_definitions) – فروشگاه مرکزی
-- ============================================================
CREATE TABLE IF NOT EXISTS item_definitions (
    item_id VARCHAR(50) PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    type VARCHAR(20) NOT NULL,          -- 'skin', 'board', 'sticker', 'consumable'
    rarity VARCHAR(20) NOT NULL,
    game_id VARCHAR(50),                -- null = عمومی, else مختص یک بازی
    mode_id VARCHAR(50),
    is_tradable BOOLEAN DEFAULT TRUE,   -- رزرو برای آینده
    is_giftable BOOLEAN DEFAULT TRUE,   -- رزرو برای آینده
    max_stack INT DEFAULT 1,
    expiration_days INT,
    max_equipped INT DEFAULT 1,
    global_max_quantity INT,            -- null = نامحدود
    price_soft BIGINT NOT NULL,
    daily_purchase_limit INT,
    min_level INT DEFAULT 0,
    refundable_minutes INT DEFAULT 5,
    metadata TEXT,                      -- JSON
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- جدول موجودی سراسری آیتم‌های محدود (Limited Edition)
-- ============================================================
CREATE TABLE IF NOT EXISTS item_global_inventory (
    item_id VARCHAR(50) PRIMARY KEY,
    total_sold INT NOT NULL DEFAULT 0,
    max_quantity INT NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- جدول موجودی کاربران (inventory_items)
-- ============================================================
CREATE TABLE IF NOT EXISTS inventory_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    item_id VARCHAR(50) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    expires_at TIMESTAMP,
    equipped BOOLEAN DEFAULT FALSE,
    obtained_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    source VARCHAR(50),                 -- 'purchase', 'gift', 'reward', 'refund'
    purchase_id VARCHAR(255),
    UNIQUE(user_id, item_id)
);

-- ============================================================
-- جدول لاگ تراکنش‌ها (inventory_transactions)
-- ============================================================
CREATE TABLE IF NOT EXISTS inventory_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    transaction_type VARCHAR(20) NOT NULL, -- 'purchase', 'refund', 'gift_sent', 'gift_received', 'trade_send', 'trade_receive', 'expire_refund'
    amount BIGINT,
    item_id VARCHAR(50),
    quantity INT,
    balance_after_soft BIGINT,
    correlation_id VARCHAR(255),
    source VARCHAR(50),
    operator_id VARCHAR(255),
    admin_note TEXT,
    bundle_id VARCHAR(50),
    idempotency_key VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- جدول ایدمپوتنسی (idempotency_store)
-- ============================================================
CREATE TABLE IF NOT EXISTS idempotency_store (
    key_hash VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,        -- 'PROCESSING', 'COMPLETED', 'FAILED'
    response_payload TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL
);

-- ============================================================
-- جدول تاریخچه هدیه (gift_history)
-- ============================================================
CREATE TABLE IF NOT EXISTS gift_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    correlation_id VARCHAR(255) NOT NULL,
    from_user_id VARCHAR(255) NOT NULL,
    to_user_id VARCHAR(255) NOT NULL,
    gift_type VARCHAR(20) NOT NULL,     -- 'coin', 'item'
    amount BIGINT,
    item_id VARCHAR(50),
    quantity INT DEFAULT 1,
    message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- جدول تاریخچه معامله (trade_history) – رزرو برای آینده
-- ============================================================
CREATE TABLE IF NOT EXISTS trade_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    correlation_id VARCHAR(255) NOT NULL,
    user_a_id VARCHAR(255) NOT NULL,
    user_b_id VARCHAR(255) NOT NULL,
    items_a TEXT,
    items_b TEXT,
    coins_a BIGINT,
    coins_b BIGINT,
    status VARCHAR(20),                -- 'proposed', 'accepted', 'rejected', 'expired'
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- جدول بسته‌های تخفیفی (bundles)
-- ============================================================
CREATE TABLE IF NOT EXISTS bundles (
    bundle_id VARCHAR(50) PRIMARY KEY,
    name TEXT NOT NULL,
    items TEXT NOT NULL,               -- JSON
    discounted_price_soft BIGINT NOT NULL,
    daily_purchase_limit INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- جدول اسنپ‌شات روزانه موجودی (برای بازیابی Point-in-Time – اختیاری)
-- ============================================================
CREATE TABLE IF NOT EXISTS inventory_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    snapshot_date DATE NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    soft_currency BIGINT NOT NULL,
    wallet_version BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- جدول لاگ سوءاستفاده از بازپرداخت
-- ============================================================
CREATE TABLE IF NOT EXISTS refund_abuse_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    item_id VARCHAR(50) NOT NULL,
    reason VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- جدول نگاشت correlation_id به transaction_ids (برای عملیات چندگانه)
-- ============================================================
CREATE TABLE IF NOT EXISTS transaction_correlation_lookup (
    correlation_id VARCHAR(255) PRIMARY KEY,
    transaction_ids TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);