-- جدول لاگ اجرای Sink خودکار (برای ممیزی)
CREATE TABLE IF NOT EXISTS economy_auto_sink_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    execution_time TIMESTAMP NOT NULL,
    inflation_rate_before DOUBLE NOT NULL,
    total_supply_before BIGINT NOT NULL,
    affected_users INT NOT NULL,
    total_coins_removed BIGINT NOT NULL,
    status VARCHAR(20),
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);