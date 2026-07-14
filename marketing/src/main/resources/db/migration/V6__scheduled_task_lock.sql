CREATE TABLE scheduled_task_lock (
    lock_name VARCHAR(128) NOT NULL,
    owner_id VARCHAR(128) NOT NULL,
    locked_until TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (lock_name),
    KEY idx_task_lock_expiry (locked_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
