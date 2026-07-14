package com.emall.common.sharding;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class ShardRouteIndexTest {
    private final ShardRouteIndex routeIndex = ShardRouteIndex.local();

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void removesPendingUniqueRouteWhenTransactionRollsBack() {
        TransactionSynchronizationManager.initSynchronization();
        routeIndex.bindUniqueTransactional("order-id", "1001", 2001L);
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();

        synchronizations.forEach(
                synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        assertThat(routeIndex.resolve("order-id", "1001")).isEmpty();
    }

    @Test
    void removesReleasedRouteBeforeCommitAndRestoresItOnRollback() {
        routeIndex.bindUnique("coupon", "coupon-1", 3001L);
        TransactionSynchronizationManager.initSynchronization();
        routeIndex.removeIfOwnedTransactional("coupon", "coupon-1", 3001L);
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();

        assertThat(routeIndex.resolve("coupon", "coupon-1")).hasValue(3001L);
        synchronizations.forEach(synchronization -> synchronization.beforeCommit(false));
        assertThat(routeIndex.resolve("coupon", "coupon-1")).isEmpty();
        synchronizations.forEach(
                synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        assertThat(routeIndex.resolve("coupon", "coupon-1")).hasValue(3001L);
    }
}
