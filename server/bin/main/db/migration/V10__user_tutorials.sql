CREATE TABLE IF NOT EXISTS user_tutorials (
    user_id VARCHAR(255) NOT NULL,
    game_id VARCHAR(50) NOT NULL,
    completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, game_id)
);