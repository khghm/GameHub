-- ============================================================
-- جدول بازی‌های تمام شده (برای آرشیو)
-- ============================================================
CREATE TABLE IF NOT EXISTS finished_games (
    game_id VARCHAR(255) PRIMARY KEY,
    game_type VARCHAR(50) NOT NULL,
    players TEXT NOT NULL,
    winner VARCHAR(255),
    draw BOOLEAN DEFAULT FALSE,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- جدول لاگ پاکسازی بازی‌های یتیم (برای مانیتورینگ)
-- ============================================================
CREATE TABLE IF NOT EXISTS game_cleanup_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    game_id VARCHAR(255) NOT NULL,
    reason VARCHAR(50),
    cleaned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);