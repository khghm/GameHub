-- ============================================================
-- فاز 7: جداول کلن و انجمن
-- ============================================================

-- 1. جدول کلن‌ها (گروه‌های دائمی)
CREATE TABLE IF NOT EXISTS clans (
    id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    tag VARCHAR(10) NOT NULL UNIQUE,
    owner_id VARCHAR(255) NOT NULL,
    level INT DEFAULT 1,
    member_count INT DEFAULT 1,
    max_members INT DEFAULT 50,
    coins_required_for_next_level BIGINT DEFAULT 0,
    total_coins_contributed BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. جدول اعضای کلن
CREATE TABLE IF NOT EXISTS clan_members (
    clan_id VARCHAR(20) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    role VARCHAR(20) DEFAULT 'member', -- owner, admin, elder, member
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    coins_contributed BIGINT DEFAULT 0,
    PRIMARY KEY (clan_id, user_id)
);

-- 3. جدول سطوح ارتقای کلن (پیکربندی)
CREATE TABLE IF NOT EXISTS clan_levels (
    level INT PRIMARY KEY,
    max_members INT NOT NULL,
    upgrade_cost_coins BIGINT NOT NULL,
    features TEXT -- JSON شامل قابلیت‌ها (مثل نقش‌های مدیریتی، پین محتوا، لیدربورد)
);

-- درج سطوح اولیه
INSERT INTO clan_levels (level, max_members, upgrade_cost_coins, features) VALUES
(1, 50, 0, '{"roles": ["member"]}'),
(2, 500, 300, '{"roles": ["member","admin","elder"], "features": ["manage_roles"]}'),
(3, 2000, 1000, '{"roles": ["member","admin","elder","owner"], "features": ["pin_messages","mute_members"]}'),
(4, 5000, 5000, '{"roles": ["member","admin","elder","owner"], "features": ["internal_leaderboard"]}'),
(5, 10000, 9000, '{"roles": ["member","admin","elder","owner"], "features": ["custom_logo"]}'),
(6, 20000, 15000, '{"roles": ["member","admin","elder","owner"], "features": ["internal_tournaments"]}');

-- 4. جدول انجمن‌ها (گروه‌های بزرگ با شرط عضویت)
CREATE TABLE IF NOT EXISTS societies (
    id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    owner_id VARCHAR(255) NOT NULL,
    member_count INT DEFAULT 0,
    max_members INT DEFAULT 50000,
    membership_type VARCHAR(20) DEFAULT 'open', -- open, approval, condition
    membership_condition TEXT, -- JSON query builder
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. جدول اعضای انجمن
CREATE TABLE IF NOT EXISTS society_members (
    society_id VARCHAR(20) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    role VARCHAR(20) DEFAULT 'member', -- owner, admin, member
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'active', -- active, pending (برای نیاز به تأیید)
    PRIMARY KEY (society_id, user_id)
);

-- 6. جدول قوانین انجمن (برای شرط عضویت)
-- (در این مرحله شرط عضویت در ستون membership_condition ذخیره می‌شود)

-- ایندکس‌ها
CREATE INDEX idx_clan_owner ON clans(owner_id);
CREATE INDEX idx_clan_member ON clan_members(user_id);
CREATE INDEX idx_society_owner ON societies(owner_id);
CREATE INDEX idx_society_member ON society_members(user_id);