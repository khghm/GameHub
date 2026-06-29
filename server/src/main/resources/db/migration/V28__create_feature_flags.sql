CREATE TABLE IF NOT EXISTS feature_flags (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    flag_key VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_enabled BOOLEAN DEFAULT FALSE,
    environment VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_feature_flags_flag_key ON feature_flags(flag_key);
CREATE INDEX IF NOT EXISTS idx_feature_flags_environment ON feature_flags(environment);
