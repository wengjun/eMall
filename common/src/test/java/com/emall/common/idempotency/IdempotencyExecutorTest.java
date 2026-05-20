package com.emall.common.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class IdempotencyExecutorTest {
    private final InMemoryIdempotencyRepository repository = new InMemoryIdempotencyRepository();
    private final IdempotencyService service =
            new IdempotencyService(repository, Clock.fixed(Instant.parse("2026-05-19T00:00:00Z"), ZoneOffset.UTC),
                    Duration.ofSeconds(30), Duration.ofDays(7));

    @Test
    void shouldMarkSuccessAndReplayCompletedResponse() {
        IdempotencyKey key = IdempotencyKey.of("order", "70001", "request-001", "create");
        String requestDigest = service.digest("create-order:70001");
        AtomicInteger executions = new AtomicInteger();

        String first = IdempotencyExecutor.execute(service, key, "ORDER", "70001", requestDigest, () -> {
            executions.incrementAndGet();
            return "created-order-70001";
        }, record -> "replayed:" + record.responseDigest(), service::digest);
        String second = IdempotencyExecutor.execute(service, key, "ORDER", "70001", requestDigest, () -> {
            executions.incrementAndGet();
            return "should-not-run";
        }, record -> "replayed:" + record.responseDigest(), service::digest);

        assertThat(first).isEqualTo("created-order-70001");
        assertThat(second).isEqualTo("replayed:" + service.digest("created-order-70001"));
        assertThat(executions).hasValue(1);
        assertThat(repository.find(key.storageKey())).get().extracting(IdempotencyRecord::status)
                .isEqualTo(IdempotencyStatus.SUCCEEDED);
    }

    @Test
    void shouldMarkBusinessExceptionAsTerminalFailure() {
        IdempotencyKey key = IdempotencyKey.of("payment", "90001", "request-002", "callback");
        String requestDigest = service.digest("callback:90001");

        assertThatThrownBy(() -> IdempotencyExecutor.execute(service, key, "PAYMENT", "90001", requestDigest, () -> {
            throw new BusinessException(ErrorCode.FORBIDDEN, "invalid signature");
        }, ignored -> "unused", service::digest)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("invalid signature");
        assertThatThrownBy(() -> IdempotencyExecutor.execute(service, key, "PAYMENT", "90001", requestDigest,
                () -> "should-not-run", ignored -> "unused", service::digest)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("failed terminally");
        assertThat(repository.find(key.storageKey())).get().extracting(IdempotencyRecord::status)
                .isEqualTo(IdempotencyStatus.TERMINAL_FAILED);
    }

    @Test
    void shouldAllowRetryAfterRuntimeException() {
        IdempotencyKey key = IdempotencyKey.of("inventory", "30001", "request-003", "reserve");
        String requestDigest = service.digest("reserve:30001");

        assertThatThrownBy(() -> IdempotencyExecutor.execute(service, key, "INVENTORY", "30001", requestDigest, () -> {
            throw new IllegalStateException("temporary downstream failure");
        }, ignored -> "unused", service::digest)).isInstanceOf(IllegalStateException.class);

        String retry = IdempotencyExecutor.execute(service, key, "INVENTORY", "30001", requestDigest, () -> "reserved",
                ignored -> "unused", service::digest);

        assertThat(retry).isEqualTo("reserved");
        assertThat(repository.find(key.storageKey())).get().extracting(IdempotencyRecord::status)
                .isEqualTo(IdempotencyStatus.SUCCEEDED);
    }
}
