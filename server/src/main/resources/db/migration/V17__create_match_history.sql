-- ============================================================
-- Create match_history table for storing completed matches
-- ============================================================
CREATE TABLE IF NOT EXISTS match_history (
    id VARCHAR(255) PRIMARY KEY,
    game_type VARCHAR(50) NOT NULL,
    players TEXT NOT NULL,
    winner VARCHAR(255),
    draw BOOLEAN DEFAULT FALSE,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index for faster queries
CREATE INDEX IF NOT EXISTS idx_match_history_timestamp ON match_history(timestamp);
CREATE INDEX IF NOT EXISTS idx_match_history_winner ON match_history(winner);