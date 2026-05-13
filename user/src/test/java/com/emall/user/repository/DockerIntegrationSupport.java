package com.emall.user.repository;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.testcontainers.DockerClientFactory;

final class DockerIntegrationSupport {
    private static final long DOCKER_CHECK_TIMEOUT_SECONDS =
            Long.getLong("emall.testcontainers.docker-check-timeout-seconds", 90L);
    private static volatile Boolean dockerAvailable;

    private DockerIntegrationSupport() {
    }

    static boolean isDockerAvailable() {
        Boolean current = dockerAvailable;
        if (current != null) {
            return current;
        }
        synchronized (DockerIntegrationSupport.class) {
            current = dockerAvailable;
            if (current == null) {
                current = checkDockerWithTimeout();
                dockerAvailable = current;
            }
            return current;
        }
    }

    private static boolean checkDockerWithTimeout() {
        ExecutorService executor = Executors.newSingleThreadExecutor(daemonThreadFactory());
        CompletableFuture<Boolean> future =
                CompletableFuture.supplyAsync(DockerIntegrationSupport::checkDocker, executor);
        try {
            return future.get(DOCKER_CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException | TimeoutException ex) {
            return false;
        } finally {
            executor.shutdownNow();
        }
    }

    private static boolean checkDocker() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable ex) {
            return false;
        }
    }

    private static ThreadFactory daemonThreadFactory() {
        return task -> {
            Thread thread = new Thread(task, "docker-availability-check");
            thread.setDaemon(true);
            return thread;
        };
    }
}
