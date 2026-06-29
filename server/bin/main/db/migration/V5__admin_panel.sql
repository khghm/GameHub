-- server/src/main/resources/db/migration/V5__admin_panel.sql

-- ============================================================================
-- 1. جدول ادمین‌ها (مدیران)
-- ============================================================================
CREATE TABLE IF NOT EXISTS admin_users (
    id UUID DEFAULT UUID() PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    role VARCHAR(20) NOT NULL,
    two_factor_secret TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP
);

-- ============================================================================
-- 2. جدول مجوزها (permissions) برای RBAC
-- ============================================================================
CREATE TABLE IF NOT EXISTS permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    permission_name VARCHAR(100) UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS role_permissions (
    role VARCHAR(20) NOT NULL,
    permission_id BIGINT NOT NULL,
    FOREIGN KEY (permission_id) REFERENCES permissions(id)
);

-- ============================================================================
-- 3. جدول لاگ اقدامات ادمین (Audit Log)
-- ============================================================================
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_id UUID,
    admin_username VARCHAR(50),
    action VARCHAR(100) NOT NULL,
    target_user_id TEXT,
    target_type VARCHAR(50),
    details TEXT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_admin ON audit_log(admin_id);
CREATE INDEX IF NOT EXISTS idx_audit_target ON audit_log(target_user_id);
CREATE INDEX IF NOT EXISTS idx_audit_time ON audit_log(created_at DESC);

-- ============================================================================
-- 4. جدول تنظیمات سیستمی (ستون value به setting_value تغییر کرد)
-- ============================================================================
CREATE TABLE IF NOT EXISTS system_settings (
    setting_key VARCHAR(100) PRIMARY KEY,
    setting_value TEXT NOT NULL,
    description TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID
);

-- ============================================================================
-- 5. جدول برای کانفیگ بازی‌ها (نسخه‌گذاری شده)
-- ============================================================================
CREATE TABLE IF NOT EXISTS game_config_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    game_id VARCHAR(50) NOT NULL,
    config_version INT NOT NULL,
    config_json TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID
);

-- ============================================================================
-- 6. جدول بلاک‌لیست IP و دستگاه
-- ============================================================================
CREATE TABLE IF NOT EXISTS blocklist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    block_type VARCHAR(20) NOT NULL,  -- 'ip', 'device'
    block_value VARCHAR(255) NOT NULL,
    reason TEXT,
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID
);

CREATE INDEX IF NOT EXISTS idx_blocklist_value ON blocklist(block_value);
CREATE INDEX IF NOT EXISTS idx_blocklist_expires ON blocklist(expires_at);