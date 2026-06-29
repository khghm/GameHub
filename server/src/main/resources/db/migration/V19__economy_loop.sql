-- ============================================================
-- V19__economy_loop.sql
-- جدول ثبت دقیقه‌ای جریان سکه (فقط برای آمار – بدون اعمال Sink)
-- ============================================================

-- 1. جدول ثبت دقیقه‌ای جریان سکه (برای محاسبه تورم – در صورت نیاز)
CREATE TABLE IF NOT EXISTS economy_supply_minute (
    minute_bucket TIMESTAMP PRIMARY KEY,
    inflow_delta BIGINT NOT NULL DEFAULT 0,
    outflow_delta BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- سایر جداول (auto_sink_log, gini_snapshot) حذف شدند