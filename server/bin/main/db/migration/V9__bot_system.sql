-- ============================================================
-- اضافه کردن فیلدهای جدید به جدول bot_profiles
-- ============================================================
ALTER TABLE bot_profiles ADD COLUMN IF NOT EXISTS last_rotation TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE bot_profiles ADD COLUMN IF NOT EXISTS last_game_at TIMESTAMP;
ALTER TABLE bot_profiles ADD COLUMN IF NOT EXISTS total_games_played INT DEFAULT 0;
ALTER TABLE bot_profiles ADD COLUMN IF NOT EXISTS win_count INT DEFAULT 0;
ALTER TABLE bot_profiles ADD COLUMN IF NOT EXISTS loss_count INT DEFAULT 0;

-- ============================================================
-- جدول تاریخچه بازی‌های ربات (برای تولید تاریخچه مصنوعی)
-- ============================================================
CREATE TABLE IF NOT EXISTS bot_match_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bot_id VARCHAR(255) NOT NULL,
    game_id VARCHAR(50) NOT NULL,
    match_id VARCHAR(255) NOT NULL,
    opponent_type VARCHAR(20), -- 'human', 'bot'
    result VARCHAR(10), -- 'win', 'loss', 'draw'
    rating_change INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- جدول آواتارهای ربات (مجموعه از پیش تعریف شده)
-- ============================================================
CREATE TABLE IF NOT EXISTS bot_avatars (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    avatar_id VARCHAR(100) UNIQUE NOT NULL,
    image_url VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE
);

-- ============================================================
-- درج آواتارهای پیش‌فرض
-- ============================================================
INSERT INTO bot_avatars (avatar_id, image_url) VALUES
('avatar_bot_1', '/avatars/bot1.png'),
('avatar_bot_2', '/avatars/bot2.png'),
('avatar_bot_3', '/avatars/bot3.png'),
('avatar_bot_4', '/avatars/bot4.png'),
('avatar_bot_5', '/avatars/bot5.png');