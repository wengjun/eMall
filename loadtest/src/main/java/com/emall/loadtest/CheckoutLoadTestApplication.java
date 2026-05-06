package com.emall.loadtest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class CheckoutLoadTestApplication {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final LoadTestOptions options;
    private final ExecutorService executor;
    private final HttpClient httpClient;

    private CheckoutLoadTestApplication(LoadTestOptions options) {
        this.options = options;
        this.executor = Executors.newFixedThreadPool(options.maxConcurrency());
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .executor(executor)
                .build();
    }

    public static void main(String[] args) throws Exception {
        LoadTestOptions options = LoadTestOptions.from(args);
        CheckoutLoadTestApplication application = new CheckoutLoadTestApplication(options);
        try {
            application.run();
        } finally {
            application.shutdown();
        }
    }

    private void run() throws InterruptedException {
        if (options.bootstrapData()) {
            bootstrapData();
        }
        long startNanos = System.nanoTime();
        long endNanos = startNanos + options.duration().toNanos();
        long intervalNanos = TimeUnit.SECONDS.toNanos(1) / options.ratePerSecond();
        AtomicInteger sequence = new AtomicInteger();
        List<CompletableFuture<Result>> futures = new ArrayList<>();

        long nextNanos = startNanos;
        while (System.nanoTime() < endNanos) {
            int requestNo = sequence.incrementAndGet();
            futures.add(sendScenarioRequest(requestNo));
            nextNanos += intervalNanos;
            sleepUntil(nextNanos);
        }

        List<Result> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();
        printReport(results, Duration.ofNanos(System.nanoTime() - startNanos));
    }

    private void bootstrapData() {
        post("/api/prices", Map.of(
                "skuId", options.skuId(),
                "listPrice", options.listPrice(),
                "salePrice", options.salePrice(),
                "currency", options.currency(),
                "active", true));
        post("/api/inventory/" + options.skuId() + "/stock", Map.of("quantity", options.bootstrapStock()));
        post("/api/search/documents", Map.of(
                "skuId", options.skuId(),
                "title", "loadtest flagship phone",
                "category", "digital",
                "price", options.salePrice(),
                "tags", List.of("phone", "loadtest", "hot"),
                "saleable", true));
    }

    private CompletableFuture<Result> sendScenarioRequest(int requestNo) {
        return switch (options.scenario()) {
            case CHECKOUT -> sendCheckout(requestNo, false);
            case HOT_SKU -> sendCheckout(requestNo, true);
            case READ_HEAVY -> sendReadHeavy(requestNo);
            case PAYMENT_CALLBACKS -> sendPaymentCallback(requestNo);
            case MQ_BACKLOG -> sendProductChange(requestNo);
        };
    }

    private CompletableFuture<Result> sendCheckout(int requestNo, boolean hotSku) {
        long started = System.nanoTime();
        String requestId = "loadtest-" + started + "-" + requestNo;
        String path = hotSku ? "/api/orders?skuId=" + options.skuId() : "/api/orders";
        return sendAsync(started, request(path)
                .header("X-Device-Id", deviceId(requestNo))
                .POST(jsonBody(Map.of(
                        "requestId", requestId,
                        "userId", hotSku ? options.userId() : options.userId() + requestNo,
                        "skuId", options.skuId(),
                        "quantity", options.quantity()))));
    }

    private CompletableFuture<Result> sendReadHeavy(int requestNo) {
        long started = System.nanoTime();
        String keyword = encode(options.keyword());
        String path = switch (requestNo % 5) {
            case 0 -> "/api/products/" + options.skuId();
            case 1 -> "/api/products?keyword=" + keyword + "&limit=20&skuId=" + options.skuId();
            case 2 -> "/api/search?keyword=" + keyword + "&limit=20&skuId=" + options.skuId();
            case 3 -> "/api/inventory/" + options.skuId();
            default -> "/api/prices/" + options.skuId();
        };
        return sendAsync(started, request(path)
                .header("X-Device-Id", deviceId(requestNo))
                .GET());
    }

    private CompletableFuture<Result> sendPaymentCallback(int requestNo) {
        long started = System.nanoTime();
        String requestId = "payment-loadtest-" + started + "-" + requestNo;
        HttpRequest createRequest = request("/api/payments")
                .header("X-Device-Id", deviceId(requestNo))
                .POST(jsonBody(Map.of(
                        "requestId", requestId,
                        "orderId", options.orderIdBase() + requestNo,
                        "userId", options.userId() + requestNo,
                        "amount", options.salePrice(),
                        "channel", options.paymentChannel())))
                .build();
        return httpClient.sendAsync(createRequest, HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> sendCallbackIfCreated(started, requestNo, response))
                .exceptionally(error -> Result.failed(elapsedMillis(started)));
    }

    private CompletableFuture<Result> sendCallbackIfCreated(long started,
                                                            int requestNo,
                                                            HttpResponse<String> response) {
        if (!isSuccess(response.statusCode())) {
            return CompletableFuture.completedFuture(Result.failed(elapsedMillis(started)));
        }
        long paymentId = paymentId(response.body());
        HttpRequest callbackRequest = request("/api/payments/" + paymentId + "/callbacks")
                .header("X-Device-Id", deviceId(requestNo))
                .POST(jsonBody(Map.of(
                        "channelTradeNo", "loadtest-trade-" + started + "-" + requestNo,
                        "paidAmount", options.salePrice())))
                .build();
        return httpClient.sendAsync(callbackRequest, HttpResponse.BodyHandlers.discarding())
                .<Result>handle((callbackResponse, error) -> toResult(started, callbackResponse, error));
    }

    private CompletableFuture<Result> sendProductChange(int requestNo) {
        long started = System.nanoTime();
        String title = "loadtest product title " + requestNo;
        return sendAsync(started, request("/api/products/" + options.skuId() + "/title")
                .header("X-Device-Id", deviceId(requestNo))
                .method("PATCH", jsonBody(Map.of("title", title))));
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create(options.baseUrl() + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json");
    }

    private HttpRequest.BodyPublisher jsonBody(Object body) {
        try {
            return HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(body));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize request body", ex);
        }
    }

    private CompletableFuture<Result> sendAsync(long started, HttpRequest.Builder request) {
        return httpClient.sendAsync(request.build(), HttpResponse.BodyHandlers.discarding())
                .<Result>handle((response, error) -> toResult(started, response, error));
    }

    private void post(String path, Object body) {
        try {
            HttpRequest request = request(path)
                    .header("X-Device-Id", "loadtest-bootstrap")
                    .POST(jsonBody(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (!isSuccess(response.statusCode())) {
                throw new IllegalStateException("POST " + path + " failed with HTTP " + response.statusCode());
            }
        } catch (IOException ex) {
            throw new IllegalStateException("POST " + path + " failed", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("POST " + path + " interrupted", ex);
        }
    }

    private Result toResult(long started, HttpResponse<?> response, Throwable error) {
        long elapsedMillis = elapsedMillis(started);
        if (error != null || response == null) {
            return Result.failed(elapsedMillis);
        }
        return new Result(isSuccess(response.statusCode()), elapsedMillis);
    }

    private long paymentId(String responseBody) {
        try {
            Map<String, Object> response = OBJECT_MAPPER.readValue(responseBody, MAP_TYPE);
            Object data = response.get("data");
            if (data instanceof Map<?, ?> dataMap) {
                return Long.parseLong(String.valueOf(dataMap.get("paymentId")));
            }
            throw new IllegalStateException("payment response does not contain data.paymentId");
        } catch (IOException ex) {
            throw new IllegalStateException("failed to parse payment response", ex);
        }
    }

    private void printReport(List<Result> results, Duration elapsed) {
        List<Long> latencies = results.stream()
                .map(Result::latencyMillis)
                .sorted(Comparator.naturalOrder())
                .toList();
        long total = results.size();
        long success = results.stream().filter(Result::success).count();
        long failed = total - success;
        double errorRate = total == 0 ? 0.0 : (double) failed / total;
        System.out.printf("%s load test completed in %d ms%n", options.scenario().cliName(), elapsed.toMillis());
        System.out.printf("requests=%d, success=%d, failed=%d, errorRate=%.4f%n", total, success, failed, errorRate);
        System.out.printf("p50=%d ms, p95=%d ms, p99=%d ms%n",
                percentile(latencies, 50), percentile(latencies, 95), percentile(latencies, 99));
        if (errorRate > options.maxErrorRate()) {
            throw new IllegalStateException("error rate exceeded threshold " + options.maxErrorRate());
        }
    }

    private long percentile(List<Long> values, int percentile) {
        if (values.isEmpty()) {
            return 0L;
        }
        int index = (int) Math.ceil(values.size() * percentile / 100.0) - 1;
        return values.get(Math.max(0, Math.min(index, values.size() - 1)));
    }

    private long elapsedMillis(long startedNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);
    }

    private String deviceId(int requestNo) {
        return "loadtest-" + (requestNo % options.maxConcurrency());
    }

    private boolean isSuccess(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void sleepUntil(long targetNanos) throws InterruptedException {
        long remainingNanos = targetNanos - System.nanoTime();
        if (remainingNanos > 0) {
            TimeUnit.NANOSECONDS.sleep(remainingNanos);
        }
    }

    private void shutdown() {
        executor.shutdown();
    }

    private enum LoadScenario {
        CHECKOUT("checkout"),
        READ_HEAVY("read-heavy"),
        HOT_SKU("hot-sku"),
        PAYMENT_CALLBACKS("payment-callbacks"),
        MQ_BACKLOG("mq-backlog");

        private final String cliName;

        LoadScenario(String cliName) {
            this.cliName = cliName;
        }

        static LoadScenario from(String value) {
            for (LoadScenario scenario : values()) {
                if (scenario.cliName.equalsIgnoreCase(value)) {
                    return scenario;
                }
            }
            throw new IllegalArgumentException("unsupported load scenario: " + value);
        }

        String cliName() {
            return cliName;
        }
    }

    private record Result(boolean success, long latencyMillis) {
        static Result failed(long latencyMillis) {
            return new Result(false, latencyMillis);
        }
    }

    private record LoadTestOptions(
            String baseUrl,
            int ratePerSecond,
            Duration duration,
            int maxConcurrency,
            LoadScenario scenario,
            long userId,
            long skuId,
            long orderIdBase,
            int quantity,
            double maxErrorRate,
            boolean bootstrapData,
            int bootstrapStock,
            BigDecimal listPrice,
            BigDecimal salePrice,
            String currency,
            String keyword,
            String paymentChannel
    ) {
        static LoadTestOptions from(String[] args) {
            return new LoadTestOptions(
                    value(args, 0, env("EMALL_BASE_URL", "http://localhost:8080")),
                    integer(args, 1, env("EMALL_LOAD_RATE", "100")),
                    Duration.ofSeconds(integer(args, 2, env("EMALL_LOAD_DURATION_SECONDS", "60"))),
                    integer(args, 3, env("EMALL_LOAD_MAX_CONCURRENCY", "200")),
                    LoadScenario.from(value(args, 4, env("EMALL_LOAD_SCENARIO", "checkout"))),
                    Long.parseLong(env("EMALL_LOAD_USER_ID", "100001")),
                    Long.parseLong(env("EMALL_LOAD_SKU_ID", "10001")),
                    Long.parseLong(env("EMALL_LOAD_ORDER_ID_BASE", "900000000")),
                    Integer.parseInt(env("EMALL_LOAD_QUANTITY", "1")),
                    Double.parseDouble(env("EMALL_LOAD_MAX_ERROR_RATE", "0.01")),
                    Boolean.parseBoolean(env("EMALL_LOAD_BOOTSTRAP_DATA", "true")),
                    Integer.parseInt(env("EMALL_LOAD_BOOTSTRAP_STOCK", "1000000")),
                    new BigDecimal(env("EMALL_LOAD_LIST_PRICE", "3999.00")),
                    new BigDecimal(env("EMALL_LOAD_SALE_PRICE", "3799.00")),
                    env("EMALL_LOAD_CURRENCY", "CNY"),
                    env("EMALL_LOAD_KEYWORD", "phone"),
                    env("EMALL_LOAD_PAYMENT_CHANNEL", "loadtest"));
        }

        private static String value(String[] args, int index, String defaultValue) {
            String value = args.length > index && !args[index].isBlank() ? args[index] : defaultValue;
            return index == 0 ? trimTrailingSlash(value) : value;
        }

        private static int integer(String[] args, int index, String defaultValue) {
            return Integer.parseInt(value(args, index, defaultValue));
        }

        private static String env(String key, String defaultValue) {
            return System.getenv().getOrDefault(key, defaultValue);
        }

        private static String trimTrailingSlash(String value) {
            return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
        }
    }
}
