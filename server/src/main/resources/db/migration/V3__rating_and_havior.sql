-- ============================================================================
-- 1. جدول رتبه‌بندی کاربران برای هر بازی
-- ============================================================================
CREATE TABLE IF NOT EXISTS user_ranks (
    user_id TEXT NOT NULL,
    game_id VARCHAR(50) NOT NULL,
    rating INTEGER NOT NULL DEFAULT 1200,
    games_played INTEGER NOT NULL DEFAULT 0,
    wins INTEGER NOT NULL DEFAULT 0,
    losses INTEGER NOT NULL DEFAULT 0,
    draws INTEGER NOT NULL DEFAULT 0,
    tier VARCHAR(20) NOT NULL DEFAULT 'Bronze',
    division INTEGER NOT NULL DEFAULT 4,
    season_id INTEGER NOT NULL DEFAULT 1,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, game_id)
);

CREATE INDEX IF NOT EXISTS idx_user_ranks_game_rating ON user_ranks(game_id, rating DESC);
CREATE INDEX IF NOT EXISTS idx_user_ranks_season ON user_ranks(season_id);

-- ============================================================================
-- 2. جدول تغییرات رتبه
-- ============================================================================
CREATE TABLE IF NOT EXISTS rank_change_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id TEXT NOT NULL,
    game_id VARCHAR(50) NOT NULL,
    match_id TEXT NOT NULL,
    old_rating INTEGER NOT NULL,
    new_rating INTEGER NOT NULL,
    change_amount INTEGER NOT NULL,
    reason VARCHAR(50) NOT NULL,
    operator_id TEXT,
    admin_note TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_rank_change_user_time ON rank_change_log(user_id, created_at DESC);
CREATE INDEX idx_rank_change_match ON rank_change_log(match_id);

-- ============================================================================
-- 3. جدول رفتار کاربر
-- ============================================================================
CREATE TABLE IF NOT EXISTS user_behavior (
    user_id TEXT PRIMARY KEY,
    behavior_score INTEGER NOT NULL DEFAULT 70,
    behavior_band VARCHAR(1) NOT NULL DEFAULT 'C',
    last_band_change TIMESTAMP,
    clean_days_count INTEGER NOT NULL DEFAULT 0,
    last_activity_date DATE,
    total_positive_events INTEGER DEFAULT 0,
    total_negative_events INTEGER DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_behavior_band ON user_behavior(behavior_band);
CREATE INDEX idx_behavior_score ON user_behavior(behavior_score);

-- ============================================================================
-- 4. جدول تاریخچه رویدادهای رفتاری
-- ============================================================================
CREATE TABLE IF NOT EXISTS behavior_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id TEXT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    delta_score INTEGER NOT NULL,
    match_id TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_behavior_events_user ON behavior_events(user_id);
CREATE INDEX idx_behavior_events_time ON behavior_events(created_at DESC);

-- ============================================================================
-- 5. جدول تاریخچه ریست فصلی
-- ============================================================================
CREATE TABLE IF NOT EXISTS season_reset_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    season_id INTEGER NOT NULL,
    game_id VARCHAR(50) NOT NULL,
    reset_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    affected_users INTEGER NOT NULL
);