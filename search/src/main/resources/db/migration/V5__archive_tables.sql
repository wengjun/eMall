CREATE TABLE processed_message_archive LIKE processed_message;

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
