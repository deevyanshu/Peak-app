CREATE TABLE profile_table (
                               user_id VARCHAR(255) NOT NULL,
                               profile_pic_url TEXT,
                               bio TEXT,
                               profile_name VARCHAR(255) NOT NULL,
                               privacy VARCHAR(50) NOT NULL DEFAULT 'PUBLIC',

                               CONSTRAINT pk_profile_table PRIMARY KEY (user_id),
                               CONSTRAINT uk_profile_table_profile_name UNIQUE (profile_name),
                               CONSTRAINT chk_profile_privacy CHECK (privacy IN ('PUBLIC', 'PRIVATE'))
);

-- Optional: Create an index on profile_name for fast lookups if searched frequently
CREATE INDEX idx_profile_name ON profile_table(profile_name);