CREATE TABLE IF NOT EXISTS user_event_interactions (
                                                       user_id BIGINT NOT NULL,
                                                       event_id BIGINT NOT NULL,
                                                       weight DOUBLE PRECISION NOT NULL,
                                                       last_ts TIMESTAMP NOT NULL,
                                                       PRIMARY KEY (user_id, event_id)
);

CREATE TABLE IF NOT EXISTS event_similarity (
                                                event_a BIGINT NOT NULL,
                                                event_b BIGINT NOT NULL,
                                                score DOUBLE PRECISION NOT NULL,
                                                updated_ts TIMESTAMP NOT NULL,
                                                PRIMARY KEY (event_a, event_b)
);

CREATE INDEX IF NOT EXISTS idx_interactions_user_ts ON user_event_interactions(user_id, last_ts DESC);
CREATE INDEX IF NOT EXISTS idx_similarity_a ON event_similarity(event_a);
CREATE INDEX IF NOT EXISTS idx_similarity_b ON event_similarity(event_b);
