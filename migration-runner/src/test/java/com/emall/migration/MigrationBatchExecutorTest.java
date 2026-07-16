package com.emall.migration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class MigrationBatchExecutorTest {
    @Test
    void executesEveryTargetInABoundedBatch() {
        MigrationTargetExecutor targetExecutor = mock(MigrationTargetExecutor.class);
        MigrationBatchExecutor batchExecutor = new MigrationBatchExecutor(targetExecutor);
        MigrationTarget first = target(0);
        MigrationTarget second = target(1);

        batchExecutor.execute(List.of(first, second), 1, Duration.ofSeconds(2));

        verify(targetExecutor).execute(first);
        verify(targetExecutor).execute(second);
    }

    @Test
    void cancelsAndFailsAStalledBatchAtTheDeadline() {
        MigrationTargetExecutor targetExecutor = mock(MigrationTargetExecutor.class);
        doAnswer(invocation -> {
            Thread.sleep(5_000);
            return null;
        }).when(targetExecutor).execute(target(0));
        MigrationBatchExecutor batchExecutor = new MigrationBatchExecutor(targetExecutor);

        assertThatThrownBy(() -> batchExecutor.execute(List.of(target(0)), 1, Duration.ofMillis(50)))
                .hasMessageContaining("migration batch failed").satisfies(exception -> org.assertj.core.api.Assertions
                        .assertThat(exception.getSuppressed()).isNotEmpty());
    }

    private MigrationTarget target(int shard) {
        String database = "emall_order_%02d".formatted(shard);
        return new MigrationTarget("order", "default", shard, database, "jdbc:mysql://mysql:3306/" + database,
                "order_migration", "secret", List.of("filesystem:/app/migrations"), "flyway_schema_history", "test",
                "batch", false, true, false, List.of(), MigrationPhase.EXPAND, false, "", "");
    }
}
