-- server/src/main/resources/db/migration/V4__anti_cheat.sql

-- ============================================================================
-- 1. جدول تلاش‌های تقلب (بدون JSONB، با TEXT)
-- ============================================================================
CREATE TABLE IF NOT EXISTS cheat_attempts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id TEXT NOT NULL,
    game_id VARCHAR(50) NOT NULL,
    match_id TEXT NOT NULL,
    violation_type VARCHAR(50) NOT NULL,
    confidence_score DOUBLE NOT NULL,
    details TEXT,  -- JSONB با TEXT جایگزین شد (در H2 معادل ندارد)
    detected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    penalized BOOLEAN DEFAULT FALSE,
    appealed BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_cheat_user ON cheat_attempts(user_id);
CREATE INDEX IF NOT EXISTS idx_cheat_match ON cheat_attempts(match_id);
CREATE INDEX IF NOT EXISTS idx_cheat_detected ON cheat_attempts(detected_at DESC);

-- ============================================================================
-- 2. جدول اعتراضات به تقلب (بدون FOREIGN KEY برای جلوگیری از خطای H2)
-- ============================================================================
CREATE TABLE IF NOT EXISTS cheat_appeals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cheat_attempt_id BIGINT NOT NULL,
    user_id TEXT NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(20) DEFAULT 'pending',
    reviewed_by TEXT,
    review_note TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_cheat_appeal_user ON cheat_appeals(user_id);
CREATE INDEX IF NOT EXISTS idx_cheat_appeal_status ON cheat_appeals(status);