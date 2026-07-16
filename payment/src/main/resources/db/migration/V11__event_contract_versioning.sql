ALTER TABLE outbox_event ADD COLUMN schema_version INT NOT NULL DEFAULT 1;
ALTER TABLE outbox_event ADD COLUMN aggregate_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE outbox_event ADD COLUMN producer VARCHAR(64) NULL;
ALTER TABLE outbox_event ADD COLUMN producer_version VARCHAR(64) NULL;
ALTER TABLE outbox_event ADD COLUMN occurred_at TIMESTAMP(6) NULL;
ALTER TABLE outbox_event ADD COLUMN trace_id VARCHAR(64) NULL;
ALTER TABLE outbox_event ADD COLUMN correlation_id VARCHAR(128) NULL;

CREATE INDEX idx_outbox_aggregate_order
    ON outbox_event (aggregate_type, aggregate_id, aggregate_version, status);
CREATE INDEX idx_outbox_occurred_at ON outbox_event (occurred_at);

CREATE TABLE outbox_aggregate_sequence (
    aggregate_key VARCHAR(256) NOT NULL,
    aggregate_version BIGINT NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (aggregate_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE outbox_event_archive ADD COLUMN schema_version INT NOT NULL DEFAULT 1;
ALTER TABLE outbox_event_archive ADD COLUMN aggregate_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE outbox_event_archive ADD COLUMN producer VARCHAR(64) NULL;
ALTER TABLE outbox_event_archive ADD COLUMN producer_version VARCHAR(64) NULL;
ALTER TABLE outbox_event_archive ADD COLUMN occurred_at TIMESTAMP(6) NULL;
ALTER TABLE outbox_event_archive ADD COLUMN trace_id VARCHAR(64) NULL;
ALTER TABLE outbox_event_archive ADD COLUMN correlation_id VARCHAR(128) NULL;

