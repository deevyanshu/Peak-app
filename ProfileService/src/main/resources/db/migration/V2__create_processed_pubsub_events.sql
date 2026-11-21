CREATE TABLE processed_pubsub_events (
                                         message_id VARCHAR(128) PRIMARY KEY,
                                         blob_name VARCHAR(512) NOT NULL,
                                         processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);