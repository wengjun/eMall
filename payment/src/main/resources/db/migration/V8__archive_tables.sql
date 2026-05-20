CREATE TABLE payment_order_archive LIKE payment_order;
CREATE TABLE payment_ledger_entry_archive LIKE payment_ledger_entry;
CREATE TABLE payment_channel_statement_archive LIKE payment_channel_statement;
CREATE TABLE payment_reconciliation_record_archive LIKE payment_reconciliation_record;
CREATE TABLE outbox_event_archive LIKE outbox_event;
CREATE TABLE idempotency_record_archive LIKE idempotency_record;

CREATE TABLE archive_checkpoint (
    checkpoint_id BIGINT NOT NULL,
    table_name VARCHAR(128) NOT NULL,
    shard_id INT NOT NULL,
    cutoff_date DATE NOT NULL,
    last_archived_at TIMESTAMP(6) NOT NULL,
    copied_rows BIGINT NOT NULL,
    deleted_rows BIGINT NOT NULL,
    PRIMARY KEY (checkpoint_id),
    UNIQUE KEY uk_archive_checkpoint_table_shard (table_name, shard_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
