package com.emall.common.sharding;

public enum ShardMigrationState {
    STABLE,
    PREPARING,
    COPYING,
    CATCHING_UP,
    VERIFYING,
    CUTOVER_PENDING,
    OBSERVING,
    CLEANUP,
    ROLLBACK_PENDING,
    ROLLED_BACK,
    FAILED;

    public boolean writeAllowed() {
        return this != CUTOVER_PENDING && this != ROLLBACK_PENDING && this != FAILED;
    }

    public boolean migrationActive() {
        return this != STABLE && this != ROLLED_BACK && this != FAILED;
    }
}
