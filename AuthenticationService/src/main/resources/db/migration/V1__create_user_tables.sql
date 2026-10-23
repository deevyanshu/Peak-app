-- 1. Main User Table
CREATE TABLE user_table (
                            user_id VARCHAR(36) PRIMARY KEY,
                            username VARCHAR(255) NOT NULL UNIQUE,
                            password VARCHAR(255) NOT NULL
);

-- 2. User Roles Table (Element Collection mapping)
CREATE TABLE user_roles (
                            user_id VARCHAR(36) NOT NULL,
                            role VARCHAR(50) NOT NULL,
                            CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id)
                                REFERENCES user_table (user_id)
                                ON DELETE CASCADE
);

-- Index for optimized join performance
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);