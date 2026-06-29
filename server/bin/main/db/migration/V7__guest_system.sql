-- ============================================================
-- 1. جدول کدهای پشتیبان مهمان (backup_codes)
-- ============================================================
CREATE TABLE IF NOT EXISTS backup_codes (
    guest_id VARCHAR(255) PRIMARY KEY,
    bcrypt_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 2. جدول تاریخچه مهاجرت (migration_audit)
-- ============================================================
CREATE TABLE IF NOT EXISTS migration_audit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    guest_id VARCHAR(255) NOT NULL,
    permanent_user_id VARCHAR(255) NOT NULL,
    snapshot TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    can_merge_again BOOLEAN DEFAULT FALSE
);

-- ============================================================
-- 3. جدول لاگ پاکسازی مهمان‌ها (guest_cleanup_log)
-- ============================================================
CREATE TABLE IF NOT EXISTS guest_cleanup_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    guest_id VARCHAR(255) NOT NULL,
    reason VARCHAR(50) NOT NULL,
    deleted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 4. اضافه کردن فیلدهای جدید به جدول users (برای مهمان)
-- ============================================================
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_guest BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS device_id_hash VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS ip_hash VARCHAR(64);
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_seen TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_migrated BOOLEAN DEFAULT FALSE;