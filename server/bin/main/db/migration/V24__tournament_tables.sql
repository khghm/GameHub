-- ============================================================
-- فاز 6: جداول تورنمنت (تکمیل شده با فیلدهای هزینه و جایزه)
-- ============================================================

-- 1. جدول تورنمنت‌ها
CREATE TABLE IF NOT EXISTS tournaments (
    id VARCHAR(20) PRIMARY KEY,
    game_id VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    format VARCHAR(20) NOT NULL,           -- 'single_elimination', 'group_stage', 'swiss'
    max_participants INT NOT NULL,
    current_participants INT DEFAULT 0,
    entry_fee_coins BIGINT DEFAULT 0,
    prize_pool_coins BIGINT DEFAULT 0,
    platform_fee_percent INT DEFAULT 10,   -- کارمزد پلتفرم
    prize_distribution TEXT,                -- JSON {"1":0.5,"2":0.3,"3":0.2}
    allowed_behavior_bands TEXT,            -- JSON ["A","B"]
    min_level INT DEFAULT 0,
    min_elo INT DEFAULT 0,
    registration_start TIMESTAMP,
    registration_end TIMESTAMP,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    status VARCHAR(20) DEFAULT 'waiting',   -- waiting, registration, in_progress, completed, cancelled
    bracket_data TEXT,                      -- JSON ذخیره براکت
    winner_id VARCHAR(255),
    created_by VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. جدول ثبت‌نام کاربران در تورنمنت
CREATE TABLE IF NOT EXISTS tournament_registrations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tournament_id VARCHAR(20) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    registration_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'registered',  -- registered, eliminated, winner, waitlist
    seed INT,
    group_index INT,
    placement INT,
    coins_won BIGINT DEFAULT 0,
    UNIQUE(tournament_id, user_id)
);

-- 3. جدول مسابقات تورنمنت
CREATE TABLE IF NOT EXISTS tournament_matches (
    id VARCHAR(50) PRIMARY KEY,
    tournament_id VARCHAR(20) NOT NULL,
    round INT NOT NULL,
    match_number INT NOT NULL,
    player_a_id VARCHAR(255),
    player_b_id VARCHAR(255),
    winner_id VARCHAR(255),
    status VARCHAR(20) DEFAULT 'pending',    -- pending, in_progress, completed, disputed
    game_session_id VARCHAR(50),
    scheduled_time TIMESTAMP,
    completed_time TIMESTAMP,
    result_data TEXT                         -- JSON ذخیره نتیجه جزئی
);

-- ایندکس‌ها
CREATE INDEX idx_tournament_status ON tournaments(status);
CREATE INDEX idx_tournament_game ON tournaments(game_id);
CREATE INDEX idx_registration_tournament ON tournament_registrations(tournament_id);
CREATE INDEX idx_match_tournament ON tournament_matches(tournament_id);
CREATE INDEX idx_match_round ON tournament_matches(tournament_id, round);