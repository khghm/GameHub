-- ============================================================
-- فاز 3: جداول فروشگاه آیتم و موجودی کاربران
-- ============================================================

-- 1. جدول تعریف آیتم‌ها (کاتالوگ)
CREATE TABLE IF NOT EXISTS item_definitions (
    item_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    type VARCHAR(30) NOT NULL,        -- 'skin', 'board', 'sticker', 'consumable'
    rarity VARCHAR(20) DEFAULT 'common',
    game_id VARCHAR(50),               -- null = عمومی, else مختص یک بازی
    mode_id VARCHAR(50),               -- null = همه مودها
    is_tradable BOOLEAN DEFAULT TRUE,
    is_giftable BOOLEAN DEFAULT TRUE,
    max_stack INT DEFAULT 1,
    expiration_days INT,               -- null = دائمی
    max_equipped INT DEFAULT 1,
    global_max_quantity INT,           -- null = نامحدود
    current_sold INT DEFAULT 0,        -- برای آیتم‌های محدود
    price_soft BIGINT NOT NULL,
    daily_purchase_limit INT,
    min_level INT DEFAULT 0,
    refundable_minutes INT DEFAULT 5,
    metadata TEXT,                     -- JSON (مثل animation, sound, etc.)
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. جدول موجودی کاربران (inventory)
CREATE TABLE IF NOT EXISTS user_inventory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    item_id VARCHAR(50) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    expires_at TIMESTAMP,
    equipped BOOLEAN DEFAULT FALSE,
    obtained_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    source VARCHAR(50),                -- 'purchase', 'gift', 'reward'
    purchase_id VARCHAR(255),          -- برای بازپرداخت
    UNIQUE(user_id, item_id)
);

-- 3. جدول تاریخچه قیمت‌گذاری پویا (برای ممیزی)
CREATE TABLE IF NOT EXISTS item_price_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id VARCHAR(50) NOT NULL,
    price_soft BIGINT NOT NULL,
    effective_from TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. ایندکس‌ها
CREATE INDEX idx_inventory_user ON user_inventory(user_id);
-- ایندکس ساده بدون شرط WHERE (برای سازگاری با H2)
CREATE INDEX idx_inventory_expires ON user_inventory(expires_at);
CREATE INDEX idx_item_game ON item_definitions(game_id);