-- حذف کامل جدول قدیمی و ایجاد مجدد با ساختار صحیح
DROP TABLE IF EXISTS match_history;

CREATE TABLE match_history (
    id VARCHAR(255) PRIMARY KEY,
    game_type VARCHAR(50) NOT NULL,
    players TEXT NOT NULL,
    winner VARCHAR(255),
    draw BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- در صورت وجود ایندکس‌های قدیمی، آنها را حذف کنید
DROP INDEX IF EXISTS idx_match_history_timestamp;
CREATE INDEX IF NOT EXISTS idx_match_history_created ON match_history(created_at);