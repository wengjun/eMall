package com.emall.loadtest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DistributedLoadTestIT {
    @TempDir
    Path reportDirectory;

    @Test
    void shouldDriveRealHttpTrafficWithBoundedInflightAndPersistReport() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        ExecutorService serverExecutor = Executors.newFixedThreadPool(4);
        server.setExecutor(serverExecutor);
        server.createContext("/", this::success);
        server.start();
        try {
            Map<String, String> environment = LoadTestOptionsTest.environment();
            environment.put("EMALL_BASE_URL", "http://127.0.0.1:" + server.getAddress().getPort());
            environment.put("EMALL_LOAD_SCENARIO", "read-heavy");
            environment.put("EMALL_LOAD_RATE", "20");
            environment.put("EMALL_LOAD_DURATION_MS", "1000");
            environment.put("EMALL_LOAD_MAX_INFLIGHT", "8");
            environment.put("EMALL_LOAD_BACKPRESSURE_TIMEOUT_MS", "1000");
            environment.put("EMALL_LOAD_MAX_SCHEDULER_LAG_MS", "10000");
            environment.put("EMALL_LOAD_REPORT_DIR", reportDirectory.toString());
            LoadTestOptions options = LoadTestOptions.from(new String[0], environment);

            WorkerReport worker;
            try (CheckoutHttpRequestDispatcher dispatcher = new CheckoutHttpRequestDispatcher(options)) {
                worker = new LoadExecutionEngine(options, dispatcher).execute();
            }
            LoadTestReportStore store = new LoadTestReportStore(reportDirectory);
            store.writeWorker(worker);
            CapacityReport capacity = new CapacityReportAggregator().aggregate(store.readWorkers());
            store.writeCapacity(capacity);

            assertThat(worker.metrics().success()).isGreaterThan(5L);
            assertThat(worker.metrics().failed()).isZero();
            assertThat(worker.metrics().peakInflight()).isLessThanOrEqualTo(8);
            assertThat(capacity.status()).isEqualTo("BASELINE_ONLY");
            assertThat(reportDirectory.resolve("test-run.capacity.json")).exists();
        } finally {
            server.stop(0);
            serverExecutor.shutdownNow();
        }
    }

    private void success(HttpExchange exchange) throws IOException {
        byte[] body = "{\"data\":{}}".getBytes(StandardCharsets.UTF_8);
        exchange.getRequestBody().readAllBytes();
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}
