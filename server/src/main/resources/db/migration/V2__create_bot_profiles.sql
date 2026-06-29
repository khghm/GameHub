-- server/src/main/resources/db/migration/V2__create_bot_profiles.sql

-- ============================================================
-- جدول پروفایل ربات‌ها
-- ============================================================
CREATE TABLE IF NOT EXISTS bot_profiles (
    bot_id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    avatar_id VARCHAR(100) NOT NULL,
    game_id VARCHAR(50) NOT NULL,
    difficulty_level INT NOT NULL,
    elo_mean DOUBLE NOT NULL,
    elo_sigma DOUBLE NOT NULL,
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
-- ایندکس‌ها برای بهبود عملکرد
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_bot_profiles_game ON bot_profiles(game_id);
CREATE INDEX IF NOT EXISTS idx_bot_profiles_active ON bot_profiles(is_active);
CREATE INDEX IF NOT EXISTS idx_bot_profiles_difficulty ON bot_profiles(game_id, difficulty_level);
CREATE INDEX IF NOT EXISTS idx_bot_profiles_rating ON bot_profiles(elo_mean);

-- ============================================================
-- درج ربات‌های مخفی اولیه برای هر 5 بازی
-- ============================================================

-- ربات‌های تیک‌تاک‌تو (Tic Tac Toe)
INSERT INTO bot_profiles (bot_id, username, avatar_id, game_id, difficulty_level, elo_mean, elo_sigma, is_shadow) VALUES
('bot_ttt_001', 'علی رضایی', 'avatar_ttt_01', 'tictactoe', 1, 800, 200, true),
('bot_ttt_002', 'سارا کریمی', 'avatar_ttt_02', 'tictactoe', 2, 900, 200, true),
('bot_ttt_003', 'محمدرضا عسگری', 'avatar_ttt_03', 'tictactoe', 3, 1050, 200, true),
('bot_ttt_004', 'زهرا حسینی', 'avatar_ttt_04', 'tictactoe', 4, 1200, 200, true),
('bot_ttt_005', 'حسین محمودی', 'avatar_ttt_05', 'tictactoe', 5, 1350, 200, true),
('bot_ttt_006', 'مریم نوروزی', 'avatar_ttt_06', 'tictactoe', 6, 1480, 200, true),
('bot_ttt_007', 'امیر کاظمی', 'avatar_ttt_07', 'tictactoe', 7, 1600, 200, true),
('bot_ttt_008', 'نگار شهیدی', 'avatar_ttt_08', 'tictactoe', 8, 1720, 200, true),
('bot_ttt_009', 'پدرام احمدی', 'avatar_ttt_09', 'tictactoe', 9, 1850, 200, true),
('bot_ttt_010', 'شیدا میرزایی', 'avatar_ttt_10', 'tictactoe', 10, 1950, 200, true);

-- ربات‌های چهارخطی (Connect Four)
INSERT INTO bot_profiles (bot_id, username, avatar_id, game_id, difficulty_level, elo_mean, elo_sigma, is_shadow) VALUES
('bot_cf_001', 'رضا محمدیان', 'avatar_cf_01', 'connectfour', 1, 850, 200, true),
('bot_cf_002', 'سعید رحمانی', 'avatar_cf_02', 'connectfour', 2, 950, 200, true),
('bot_cf_003', 'نازنین عبداللهی', 'avatar_cf_03', 'connectfour', 3, 1100, 200, true),
('bot_cf_004', 'مجید صفری', 'avatar_cf_04', 'connectfour', 4, 1250, 200, true),
('bot_cf_005', 'لیلا کریمیان', 'avatar_cf_05', 'connectfour', 5, 1400, 200, true),
('bot_cf_006', 'بهرام احمدی', 'avatar_cf_06', 'connectfour', 6, 1520, 200, true),
('bot_cf_007', 'سوسن فرهادی', 'avatar_cf_07', 'connectfour', 7, 1650, 200, true),
('bot_cf_008', 'کیانوش رضایی', 'avatar_cf_08', 'connectfour', 8, 1750, 200, true),
('bot_cf_009', 'ترانه موسوی', 'avatar_cf_09', 'connectfour', 9, 1880, 200, true),
('bot_cf_010', 'آرش نوری', 'avatar_cf_10', 'connectfour', 10, 1980, 200, true);

-- ربات‌های اونو (Uno)
INSERT INTO bot_profiles (bot_id, username, avatar_id, game_id, difficulty_level, elo_mean, elo_sigma, is_shadow) VALUES
('bot_uno_001', 'سپیده شریفی', 'avatar_uno_01', 'uno', 1, 820, 200, true),
('bot_uno_002', 'علیرضا مظاهری', 'avatar_uno_02', 'uno', 2, 920, 200, true),
('bot_uno_003', 'فاطمه قاسمی', 'avatar_uno_03', 'uno', 3, 1080, 200, true),
('bot_uno_004', 'حمیدرضا صادقی', 'avatar_uno_04', 'uno', 4, 1220, 200, true),
('bot_uno_005', 'فرزانه جعفری', 'avatar_uno_05', 'uno', 5, 1380, 200, true),
('bot_uno_006', 'محمود زمانی', 'avatar_uno_06', 'uno', 6, 1500, 200, true),
('bot_uno_007', 'نرگس احمدی', 'avatar_uno_07', 'uno', 7, 1620, 200, true),
('bot_uno_008', 'شهرام رضایی', 'avatar_uno_08', 'uno', 8, 1730, 200, true),
('bot_uno_009', 'پریسا جمالی', 'avatar_uno_09', 'uno', 9, 1860, 200, true),
('bot_uno_010', 'کامران عبدی', 'avatar_uno_10', 'uno', 10, 1960, 200, true);

-- ربات‌های لودو (Ludo)
INSERT INTO bot_profiles (bot_id, username, avatar_id, game_id, difficulty_level, elo_mean, elo_sigma, is_shadow) VALUES
('bot_ludo_001', 'محسن بیات', 'avatar_ludo_01', 'ludo', 1, 780, 200, true),
('bot_ludo_002', 'سمیه غفاری', 'avatar_ludo_02', 'ludo', 2, 880, 200, true),
('bot_ludo_003', 'احسان هاشمی', 'avatar_ludo_03', 'ludo', 3, 1020, 200, true),
('bot_ludo_004', 'مینا کاظمی', 'avatar_ludo_04', 'ludo', 4, 1180, 200, true),
('bot_ludo_005', 'بهزاد یوسفی', 'avatar_ludo_05', 'ludo', 5, 1320, 200, true),
('bot_ludo_006', 'زینب رضاییان', 'avatar_ludo_06', 'ludo', 6, 1450, 200, true),
('bot_ludo_007', 'ایمان صالحی', 'avatar_ludo_07', 'ludo', 7, 1580, 200, true),
('bot_ludo_008', 'شیوا محمدی', 'avatar_ludo_08', 'ludo', 8, 1680, 200, true),
('bot_ludo_009', 'سجاد علیزاده', 'avatar_ludo_09', 'ludo', 9, 1800, 200, true),
('bot_ludo_010', 'آزاده غلامی', 'avatar_ludo_10', 'ludo', 10, 1900, 200, true);

-- ربات‌های مونوپولی (Monopoly)
INSERT INTO bot_profiles (bot_id, username, avatar_id, game_id, difficulty_level, elo_mean, elo_sigma, is_shadow) VALUES
('bot_mono_001', 'جواد عباسی', 'avatar_mono_01', 'monopoly', 1, 830, 200, true),
('bot_mono_002', 'مهسا عسگریان', 'avatar_mono_02', 'monopoly', 2, 930, 200, true),
('bot_mono_003', 'پویا کریمی', 'avatar_mono_03', 'monopoly', 3, 1090, 200, true),
('bot_mono_004', 'دلارام نجفی', 'avatar_mono_04', 'monopoly', 4, 1230, 200, true),
('bot_mono_005', 'هادی معینی', 'avatar_mono_05', 'monopoly', 5, 1390, 200, true),
('bot_mono_006', 'سحر همتی', 'avatar_mono_06', 'monopoly', 6, 1510, 200, true),
('bot_mono_007', 'علیرضا سلطانی', 'avatar_mono_07', 'monopoly', 7, 1630, 200, true),
('bot_mono_008', 'نازلی فتحی', 'avatar_mono_08', 'monopoly', 8, 1740, 200, true),
('bot_mono_009', 'سینا بخشی', 'avatar_mono_09', 'monopoly', 9, 1870, 200, true),
('bot_mono_010', 'هانیه مرادی', 'avatar_mono_10', 'monopoly', 10, 1970, 200, true);

-- ============================================================
-- درج یک ربات آموزشی نمونه (اختیاری)
-- ============================================================
INSERT INTO bot_profiles (bot_id, username, avatar_id, game_id, difficulty_level, elo_mean, elo_sigma, is_tutorial, is_shadow) VALUES
('bot_tutorial_general', 'ربات آموزشی', 'avatar_tutorial', 'tictactoe', 1, 700, 200, true, false);

-- ============================================================
-- به‌روزرسانی آمار برای برخی ربات‌های پرکاربرد (اختیاری)
-- ============================================================
UPDATE bot_profiles SET total_games = 500, wins = 350, losses = 150 WHERE bot_id = 'bot_ttt_005';
UPDATE bot_profiles SET total_games = 450, wins = 300, losses = 150 WHERE bot_id = 'bot_cf_005';
UPDATE bot_profiles SET total_games = 400, wins = 280, losses = 120 WHERE bot_id = 'bot_uno_005';
UPDATE bot_profiles SET total_games = 350, wins = 200, losses = 150 WHERE bot_id = 'bot_ludo_005';
UPDATE bot_profiles SET total_games = 380, wins = 250, losses = 130 WHERE bot_id = 'bot_mono_005';