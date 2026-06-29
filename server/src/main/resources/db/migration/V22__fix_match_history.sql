-- حذف جدول قدیمی و ایجاد مجدد با نام ستون صحیح
DROP TABLE IF EXISTS match_history;

CREATE TABLE match_history (
    id VARCHAR(255) PRIMARY KEY,
    game_type VARCHAR(50) NOT NULL,
    players TEXT NOT NULL,
    winner VARCHAR(255),
    draw BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);