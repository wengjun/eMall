package com.emall.identity;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
            if (!current && Boolean.getBoolean("emall.integration.require-docker")) {
                throw new IllegalStateException("Docker is required for production integration tests");
            }
            return current;
        }
    }

    private static boolean checkDockerWithTimeout() {
        ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "identity-docker-availability-check");
            thread.setDaemon(true);
            return thread;
        });
        CompletableFuture<Boolean> future =
                CompletableFuture.supplyAsync(DockerIntegrationSupport::checkDocker, executor);
        try {
            return future.get(DOCKER_CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException | TimeoutException exception) {
            return false;
        } finally {
            executor.shutdownNow();
        }
    }

    private static boolean checkDocker() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable exception) {
            return false;
        }
    }
}
