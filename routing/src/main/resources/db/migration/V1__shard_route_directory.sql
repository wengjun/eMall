CREATE TABLE global_shard_route (
    route_id VARCHAR(130) NOT NULL,
    namespace VARCHAR(64) NOT NULL,
    lookup_hash CHAR(64) NOT NULL,
    shard_key BIGINT NOT NULL,
    route_version BIGINT NOT NULL,
    expires_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (route_id),
    UNIQUE KEY uk_shard_route_lookup (namespace, lookup_hash),
    KEY idx_shard_route_expiration (expires_at),
    KEY idx_shard_route_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
