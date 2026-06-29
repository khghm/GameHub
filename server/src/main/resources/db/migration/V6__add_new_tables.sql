-- ============================================================
-- 1. جدول رنکینگ کاربران (user_ranks)
-- ============================================================
CREATE TABLE IF NOT EXISTS user_ranks (
    user_id VARCHAR(255) NOT NULL,
    game_id VARCHAR(50) NOT NULL,
    rating INT DEFAULT 1200,
    games_played INT DEFAULT 0,
    wins INT DEFAULT 0,
    losses INT DEFAULT 0,
    draws INT DEFAULT 0,
    tier VARCHAR(20) DEFAULT 'Bronze',
    division INT DEFAULT 4,
    season_id INT DEFAULT 1,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, game_id)
);

-- ============================================================
-- 2. جدول تغییرات رنکینگ (rank_change_log)
-- ============================================================
CREATE TABLE IF NOT EXISTS rank_change_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    game_id VARCHAR(50) NOT NULL,
    match_id VARCHAR(255) NOT NULL,
    old_rating INT NOT NULL,
    new_rating INT NOT NULL,
    change_amount INT NOT NULL,
    reason VARCHAR(50) NOT NULL,
    operator_id VARCHAR(255),
    admin_note TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 3. جدول خلاصه روزانه رنکینگ (rank_change_summary)
-- ============================================================
CREATE TABLE IF NOT EXISTS rank_change_summary (
    user_id VARCHAR(255) NOT NULL,
    game_id VARCHAR(50) NOT NULL,
    summary_date DATE NOT NULL,
    first_rating INT,
    last_rating INT,
    max_rating INT,
    min_rating INT,
    games_played INT,
    PRIMARY KEY (user_id, game_id, summary_date)
);

-- ============================================================
-- 4. جدول رفتار کاربر (user_behavior) – اصلاح شده
-- ============================================================
CREATE TABLE IF NOT EXISTS user_behavior (
    user_id VARCHAR(255) PRIMARY KEY,
    behavior_score INT DEFAULT 70,
    behavior_band VARCHAR(1) DEFAULT 'C',
    last_band_change TIMESTAMP,
    clean_days_count INT DEFAULT 0,
    last_activity_date DATE,
    total_positive_events INT DEFAULT 0,
    total_negative_events INT DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 5. جدول رویدادهای رفتاری (behavior_events)
-- ============================================================
CREATE TABLE IF NOT EXISTS behavior_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    delta_score INT NOT NULL,
    match_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 6. جدول تلاش‌های تقلب (cheat_attempts)
-- ============================================================
CREATE TABLE IF NOT EXISTS cheat_attempts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    game_id VARCHAR(50) NOT NULL,
    match_id VARCHAR(255),
    violation_type VARCHAR(50) NOT NULL,
    severity INT,
    confidence_score DOUBLE DEFAULT 1.0,
    evidence_url TEXT,
    detected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sanctioned BOOLEAN DEFAULT FALSE
);

-- ============================================================
-- 7. جدول تعریف آیتم‌ها (item_definitions)
-- ============================================================
CREATE TABLE IF NOT EXISTS item_definitions (
    item_id VARCHAR(50) PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    type VARCHAR(20) NOT NULL,
    rarity VARCHAR(20) NOT NULL,
    game_id VARCHAR(50),
    mode_id VARCHAR(50),
    is_tradable BOOLEAN DEFAULT TRUE,
    is_giftable BOOLEAN DEFAULT TRUE,
    max_stack INT DEFAULT 1,
    expiration_days INT,
    max_equipped INT DEFAULT 1,
    global_max_quantity INT,
    price_soft BIGINT NOT NULL,
    daily_purchase_limit INT,
    min_level INT DEFAULT 0,
    refundable_minutes INT DEFAULT 5,
    metadata TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 8. جدول موجودی کاربران (inventory_items)
-- ============================================================
CREATE TABLE IF NOT EXISTS inventory_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    item_id VARCHAR(50) NOT NULL,
    quantity INT DEFAULT 1,
    expires_at TIMESTAMP,
    equipped BOOLEAN DEFAULT FALSE,
    obtained_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    source VARCHAR(50),
    purchase_id VARCHAR(255),
    UNIQUE(user_id, item_id)
);

-- ============================================================
-- 9. جدول تراکنش‌های مالی (inventory_transactions)
-- ============================================================
CREATE TABLE IF NOT EXISTS inventory_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
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
-- 10. جدول ایدمپوتنسی (idempotency_store)
-- ============================================================
CREATE TABLE IF NOT EXISTS idempotency_store (
    key_hash VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    response_payload TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL
);

-- ============================================================
-- 11. جدول هدیه‌ها (gift_history)
-- ============================================================
CREATE TABLE IF NOT EXISTS gift_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    correlation_id VARCHAR(255) NOT NULL,
    from_user_id VARCHAR(255) NOT NULL,
    to_user_id VARCHAR(255) NOT NULL,
    gift_type VARCHAR(20) NOT NULL,
    amount BIGINT,
    item_id VARCHAR(50),
    quantity INT DEFAULT 1,
    message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 12. جدول معاملات (trade_history)
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
    status VARCHAR(20),
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 13. جدول چالش‌های روزانه (daily_challenges)
-- ============================================================
CREATE TABLE IF NOT EXISTS daily_challenges (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    game_id VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    condition_type VARCHAR(50) NOT NULL,
    condition_params TEXT NOT NULL,
    allowed_scopes TEXT NOT NULL,
    reward_type VARCHAR(50) NOT NULL,
    reward_value TEXT NOT NULL,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 14. جدول تعریف مدال‌ها (medal_definitions)
-- ============================================================
CREATE TABLE IF NOT EXISTS medal_definitions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    medal_id VARCHAR(100) UNIQUE NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    image_url TEXT,
    condition_type VARCHAR(50) NOT NULL,
    condition_params TEXT NOT NULL,
    reward_type VARCHAR(50),
    reward_value TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 15. جدول مدال‌های کاربر (user_medals)
-- ============================================================
CREATE TABLE IF NOT EXISTS user_medals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    medal_id VARCHAR(100) NOT NULL,
    earned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    display_data TEXT,
    UNIQUE(user_id, medal_id)
);

-- ============================================================
-- 16. جدول ربات‌ها (bot_profiles)
-- ============================================================
CREATE TABLE IF NOT EXISTS bot_profiles (
    bot_id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    avatar_id VARCHAR(100) NOT NULL,
    game_id VARCHAR(50) NOT NULL,
    difficulty_level INT NOT NULL,
    elo_mean DOUBLE DEFAULT 1200.0,
    elo_sigma DOUBLE DEFAULT 200.0,
    total_games INT DEFAULT 0,
    wins INT DEFAULT 0,
    losses INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    is_tutorial BOOLEAN DEFAULT FALSE,
    is_shadow BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 17. جدول اعتراضات (appeal_requests)
-- ============================================================
CREATE TABLE IF NOT EXISTS appeal_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    match_id VARCHAR(255) NOT NULL,
    game_id VARCHAR(50) NOT NULL,
    reason VARCHAR(50) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'pending',
    verdict VARCHAR(20),
    new_winner_id VARCHAR(255),
    new_elo_change TEXT,
    jury_required_elo_min INT,
    jury_required_elo_max INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    second_level_requested BOOLEAN DEFAULT FALSE,
    UNIQUE(match_id, user_id)
);

-- ============================================================
-- 18. جدول رأی هیئت منصفه (jury_votes)
-- ============================================================
CREATE TABLE IF NOT EXISTS jury_votes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    appeal_id BIGINT NOT NULL,
    juror_id VARCHAR(255) NOT NULL,
    vote_commit VARCHAR(64),
    vote_reveal VARCHAR(20),
    weight FLOAT DEFAULT 1.0,
    voted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    revealed_at TIMESTAMP,
    UNIQUE(appeal_id, juror_id)
);