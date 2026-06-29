-- ============================================================
-- جدول قوانین هشدار
-- ============================================================
CREATE TABLE IF NOT EXISTS alert_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    metric VARCHAR(100) NOT NULL,
    metric_filter VARCHAR(255),
    condition VARCHAR(10) NOT NULL,
    threshold DOUBLE NOT NULL,
    duration_seconds INT NOT NULL DEFAULT 60,
    channels VARCHAR(255) NOT NULL,
    severity VARCHAR(20) DEFAULT 'warning',
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- جدول تاریخچه هشدارها
-- ============================================================
CREATE TABLE IF NOT EXISTS alert_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id BIGINT NOT NULL,
    metric_value DOUBLE NOT NULL,
    message TEXT,
    channels_sent VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (rule_id) REFERENCES alert_rules(id) ON DELETE CASCADE
);

-- ============================================================
-- درج چند قانون پیش‌فرض
-- ============================================================
INSERT INTO alert_rules (name, metric, condition, threshold, duration_seconds, channels, severity) VALUES
('کمبود کاربر آنلاین', 'onlineHubUsers', '<', 1, 300, 'slack,telegram', 'warning'),
('بار زیاد بازی‌های فعال', 'activeGames', '>', 100, 60, 'slack', 'critical'),
('صف مچ‌میکینگ شلوغ (شطرنج کژوال)', 'matchmaking_queue_size', '>', 20, 120, 'slack', 'warning');