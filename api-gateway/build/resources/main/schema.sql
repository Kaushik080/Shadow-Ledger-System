-- Users table for API Gateway authentication
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('user', 'auditor', 'admin')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for faster lookups
CREATE INDEX IF NOT EXISTS idx_users_user_id ON users(user_id);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);

-- Insert demo users for testing (passwords are stored as plain text - NOT RECOMMENDED FOR PRODUCTION)
-- Note: In production, passwords should ALWAYS be hashed
INSERT INTO users (user_id, username, password_hash, role)
VALUES
    ('user1', 'demouser', 'password', 'user'),
    ('auditor1', 'demoauditor', 'password', 'auditor'),
    ('admin1', 'demoadmin', 'password', 'admin')
ON CONFLICT (user_id) DO NOTHING;

