package com.emall.common.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.exception.BusinessException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class IdempotencyServiceTest {
    private final InMemoryIdempotencyRepository repository = new InMemoryIdempotencyRepository();
    private final IdempotencyService service =
            new IdempotencyService(repository, Clock.fixed(Instant.parse("2026-05-19T00:00:00Z"), ZoneOffset.UTC),
                    Duration.ofSeconds(30), Duration.ofDays(7));

    @Test
    void shouldStartOnceAndReturnInProgressForDuplicateConcurrentRequest() throws Exception {
        IdempotencyKey key = IdempotencyKey.of("order", "70001", "request-001", "create");
        String digest = service.digest("order:70001:30001:1");
        List<Callable<IdempotencyStartStatus>> tasks = new ArrayList<>();
        for (int index = 0; index < 24; index++) {
            tasks.add(() -> service.begin(key, "ORDER", "70001", digest).status());
        }

        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<IdempotencyStartStatus> statuses = executor.invokeAll(tasks).stream().map(future -> {
                try {
                    return future.get();
                } catch (Exception ex) {
                    throw new AssertionError(ex);
                }
            }).toList();

            assertThat(statuses).containsExactlyInAnyOrderElementsOf(expectedStatuses(1, 23));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldReplayCompletedRecordForSameDigest() {
        IdempotencyKey key = IdempotencyKey.of("payment", "90001", "request-002", "create");
        String digest = service.digest("payment:90001:100.00");
        IdempotencyStartResult started = service.begin(key, "PAYMENT", "90001", digest);

        service.markSucceeded(started.record().key(), service.digest("payment-created:90001"));

        IdempotencyStartResult duplicate = service.begin(key, "PAYMENT", "90001", digest);
        assertThat(duplicate.status()).isEqualTo(IdempotencyStartStatus.REPLAY_SUCCEEDED);
        assertThat(duplicate.shouldExecute()).isFalse();
    }

    @Test
    void shouldRejectSameKeyWithDifferentDigest() {
        IdempotencyKey key = IdempotencyKey.of("inventory", "30001", "request-003", "reserve");
        service.begin(key, "INVENTORY", "30001", service.digest("sku=30001&quantity=1"));

        assertThatThrownBy(() -> service.begin(key, "INVENTORY", "30001", service.digest("sku=30001&quantity=2")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("different request");
    }

    private List<IdempotencyStartStatus> expectedStatuses(int started, int inProgress) {
        List<IdempotencyStartStatus> statuses = new ArrayList<>();
        for (int index = 0; index < started; index++) {
            statuses.add(IdempotencyStartStatus.STARTED);
        }
        for (int index = 0; index < inProgress; index++) {
            statuses.add(IdempotencyStartStatus.IN_PROGRESS);
        }
        return statuses;
    }
}
