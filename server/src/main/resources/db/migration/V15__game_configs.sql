-- ============================================================
-- Create game_configs table for dynamic game settings
-- ============================================================
CREATE TABLE IF NOT EXISTS game_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    game_id VARCHAR(50) NOT NULL,
    mode VARCHAR(20) NOT NULL,
    config_json TEXT NOT NULL,
    version INT NOT NULL DEFAULT 1,
    is_active BOOLEAN DEFAULT TRUE,
    created_by VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index for fast lookup of active config
CREATE INDEX IF NOT EXISTS idx_game_configs_active ON game_configs(game_id, mode, is_active);