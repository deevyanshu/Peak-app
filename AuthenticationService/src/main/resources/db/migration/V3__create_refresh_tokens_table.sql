-- Create refresh_tokens table
CREATE TABLE refresh_tokens (
                                token_id VARCHAR(36) NOT NULL,
                                refresh_token VARCHAR(255) NOT NULL,
                                expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,
                                user_id VARCHAR(36),

                                CONSTRAINT pk_refresh_tokens PRIMARY KEY (token_id),
                                CONSTRAINT uk_refresh_tokens_refresh_token UNIQUE (refresh_token),
                                CONSTRAINT fk_refresh_tokens_on_user FOREIGN KEY (user_id) REFERENCES user_table (user_id) ON DELETE CASCADE
);

-- Create index on refresh_token column
CREATE INDEX idx_refresh_token ON refresh_tokens (refresh_token);