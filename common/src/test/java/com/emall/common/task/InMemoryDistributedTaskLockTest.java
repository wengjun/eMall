package com.emall.common.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class InMemoryDistributedTaskLockTest {
    @Test
    void shouldExecuteTaskWhenLockIsAcquired() {
        DistributedTaskLock taskLock = new InMemoryDistributedTaskLock(
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC), "node-a");

        int result = taskLock.executeIfAcquired("order.compensation", Duration.ofSeconds(30), () -> 7);

        assertThat(result).isEqualTo(7);
        assertThat(taskLock.tryLock("order.compensation", Duration.ofSeconds(30))).isTrue();
    }
}
