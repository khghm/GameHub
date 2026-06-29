CREATE TABLE IF NOT EXISTS game_event_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE, game_session_id UUID NOT NULL, game_type VARCHAR(50) NOT NULL, event_type VARCHAR(100) NOT NULL, player_id VARCHAR(255), event_timestamp TIMESTAMP WITH TIME ZONE NOT NULL, sequence_number BIGINT NOT NULL, payload TEXT NOT NULL, is_applied BOOLEAN DEFAULT FALSE, applied_at TIMESTAMP WITH TIME ZONE, checksum VARCHAR(64) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_game_event_log_session ON game_event_log(game_session_id);
CREATE INDEX IF NOT EXISTS idx_game_event_log_player ON game_event_log(player_id);
CREATE INDEX IF NOT EXISTS idx_game_event_log_sequence ON game_event_log(game_session_id, sequence_number);
CREATE INDEX IF NOT EXISTS idx_game_event_log_event_timestamp ON game_event_log(event_timestamp);
