-- ============================================================
-- V26__market_data_collection.sql
-- ایجاد جداول جمع‌آوری داده‌های بازار (نسخه H2)
-- ============================================================

-- 1. جدول ذخیره اسنپ‌شوت‌های تقاضا برای هر آیتم
CREATE TABLE IF NOT EXISTS market_demand_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id VARCHAR(50) NOT NULL,
    snapshot_time TIMESTAMP NOT NULL,
    quantity_sold INT NOT NULL,
    average_price_sold BIGINT NOT NULL,
    total_revenue BIGINT NOT NULL,
    unique_buyers INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ایندکس‌ها
CREATE INDEX IF NOT EXISTS idx_item_time ON market_demand_snapshot(item_id, snapshot_time);
CREATE INDEX IF NOT EXISTS idx_created ON market_demand_snapshot(created_at);

-- 2. جدول ذخیره قیمت‌های پیشنهادی
CREATE TABLE IF NOT EXISTS market_suggested_prices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id VARCHAR(50) NOT NULL,
    suggested_price BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    demand_factor DOUBLE NOT NULL,
    current_price BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_suggested_item ON market_suggested_prices(item_id);
CREATE INDEX IF NOT EXISTS idx_suggested_created ON market_suggested_prices(created_at);

-- 3. جدول تنظیمات شبیه‌ساز بازار (در system_settings وجود دارد، فقط در صورت نیاز مقداردهی)
MERGE INTO system_settings (setting_key, setting_value, description) KEY(setting_key) VALUES
('market_simulator_enabled', 'false', 'فعال/غیرفعال بودن شبیه‌ساز بازار (true/false)'),
('market_simulator_interval_hours', '24', 'بازه زمانی جمع‌آوری داده به ساعت'),
('market_simulator_demand_factor_base', '0.5', 'ضریب حساسیت پایه به تقاضا'),
('market_simulator_min_price_ratio', '0.8', 'حداقل نسبت قیمت به قیمت پایه'),
('market_simulator_max_price_ratio', '2.0', 'حداکثر نسبت قیمت به قیمت پایه'),
('market_simulator_expected_sales_per_interval', '50', 'تعداد فروش عادی مورد انتظار در هر بازه');

-- 4. جدول لاگ اجراهای شبیه‌ساز
CREATE TABLE IF NOT EXISTS market_simulator_execution_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    execution_time TIMESTAMP NOT NULL,
    duration_ms BIGINT NOT NULL,
    items_processed INT NOT NULL,
    snapshots_created INT NOT NULL,
    suggested_prices_created INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_exec_log_time ON market_simulator_execution_log(execution_time);