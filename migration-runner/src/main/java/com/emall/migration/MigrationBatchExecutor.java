package com.emall.migration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class MigrationBatchExecutor {
    private final MigrationTargetExecutor targetExecutor;

    public MigrationBatchExecutor(MigrationTargetExecutor targetExecutor) {
        this.targetExecutor = targetExecutor;
    }

    public void execute(List<MigrationTarget> targets, int maxParallelism, Duration timeout) {
        int threadCount = Math.min(maxParallelism, targets.size());
        ExecutorService workers = Executors.newFixedThreadPool(threadCount);
        try {
            List<Callable<Void>> tasks = targets.stream().<Callable<Void>>map(target -> () -> {
                targetExecutor.execute(target);
                return null;
            }).toList();
            List<Future<Void>> results = workers.invokeAll(tasks, timeout.toMillis(), TimeUnit.MILLISECONDS);
            List<Throwable> failures = new ArrayList<>();
            for (Future<Void> result : results) {
                if (result.isCancelled()) {
                    failures.add(new IllegalStateException("migration target exceeded the batch timeout"));
                    continue;
                }
                try {
                    result.get();
                } catch (ExecutionException exception) {
                    failures.add(exception.getCause());
                }
            }
            if (!failures.isEmpty()) {
                IllegalStateException batchFailure = new IllegalStateException(
                        "migration batch failed for %d of %d targets".formatted(failures.size(), targets.size()));
                failures.forEach(batchFailure::addSuppressed);
                throw batchFailure;
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("migration batch was interrupted", exception);
        } finally {
            workers.shutdownNow();
        }
    }
}
