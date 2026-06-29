-- ============================================================
-- Create reports table
-- ============================================================
CREATE TABLE IF NOT EXISTS reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reporter_id VARCHAR(255) NOT NULL,
    reported_user_id VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'user',
    reason TEXT NOT NULL,
    details TEXT,
    evidence_url TEXT,
    reporter_score_snapshot INT,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    violation_type VARCHAR(30) NOT NULL DEFAULT 'other',
    decision VARCHAR(20),
    moderator_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP
);

-- ============================================================
-- Create indexes for better query performance
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_reports_status ON reports(status);
CREATE INDEX IF NOT EXISTS idx_reports_reporter ON reports(reporter_id);
CREATE INDEX IF NOT EXISTS idx_reports_reported ON reports(reported_user_id);
CREATE INDEX IF NOT EXISTS idx_reports_created ON reports(created_at);

-- ============================================================
-- Create report_actions table (for audit log of actions on reports)
-- ============================================================
CREATE TABLE IF NOT EXISTS report_actions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id BIGINT NOT NULL,
    admin_id VARCHAR(255) NOT NULL,
    action VARCHAR(50) NOT NULL,
    details TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_report_actions_report ON report_actions(report_id);