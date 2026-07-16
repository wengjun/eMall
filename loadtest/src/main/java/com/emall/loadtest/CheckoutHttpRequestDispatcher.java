package com.emall.loadtest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

final class CheckoutHttpRequestDispatcher implements RequestDispatcher, AutoCloseable {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final LoadTestOptions options;
    private final TrafficModel trafficModel;
    private final IdentityFixtureSource identityFixtures;
    private final HttpClient httpClient;

    CheckoutHttpRequestDispatcher(LoadTestOptions options) {
        this.options = options;
        this.trafficModel = new TrafficModel(options);
        if (trafficModel.includes(LoadScenario.PAYMENT_CALLBACKS)
                && options.paymentCallbackSecret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("payment callback load requires a secret of at least 32 UTF-8 bytes");
        }
        this.identityFixtures = new IdentityFixtureSource(options, trafficModel);
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER).version(HttpClient.Version.HTTP_2).build();
    }

    void bootstrapData() {
        post("/api/prices", Map.of("skuId", options.skuId(), "listPrice", options.listPrice(), "salePrice",
                options.salePrice(), "currency", options.currency(), "active", true));
        post("/api/inventory/" + options.skuId() + "/stock",
                Map.of("requestId", "loadtest-stock-" + options.runId(), "quantity", options.bootstrapStock()));
        post("/api/search/documents",
                Map.of("skuId", options.skuId(), "title", "loadtest flagship phone", "category", "digital", "price",
                        options.salePrice(), "tags", List.of("phone", "loadtest", "hot"), "saleable", true));
    }

    @Override
    public CompletionStage<RequestResult> dispatch(long globalSequence, LoadPattern.StageDefinition stage) {
        return switch (trafficModel.scenario(globalSequence)) {
            case CHECKOUT -> sendCheckout(globalSequence, false);
            case HOT_SKU -> sendCheckout(globalSequence, true);
            case READ_HEAVY -> sendReadHeavy(globalSequence);
            case PAYMENT_CALLBACKS -> sendPaymentCallback(globalSequence);
            case MQ_BACKLOG -> sendProductChange(globalSequence);
            case FLASH_SALE_HOTSPOT -> sendFlashSaleHotspot(globalSequence);
            case PRODUCTION_MIX ->
                throw new IllegalStateException("production mix must resolve to a concrete scenario");
        };
    }

    private CompletionStage<RequestResult> sendCheckout(long sequence, boolean hotSku) {
        long started = System.nanoTime();
        IdentityFixtureSource.Credential credential = identityFixtures.next(sequence);
        long skuId = trafficModel.skuId(sequence, hotSku);
        String requestId = requestId("order", sequence);
        String path = hotSku ? "/api/orders?skuId=" + skuId : "/api/orders";
        return sendAsync(started, request(path, sequence, credential.token()).POST(jsonBody(Map.of("requestId",
                requestId, "userId", credential.userId(), "skuId", skuId, "quantity", options.quantity()))));
    }

    private CompletionStage<RequestResult> sendReadHeavy(long sequence) {
        long started = System.nanoTime();
        long skuId = trafficModel.skuId(sequence, false);
        String keyword = encode(options.keyword());
        String path = switch ((int) Math.floorMod(sequence, 5L)) {
            case 0 -> "/api/products/" + skuId;
            case 1 -> "/api/search?keyword=&limit=20&skuId=" + skuId;
            case 2 -> "/api/search?keyword=" + keyword + "&limit=20&skuId=" + skuId;
            case 3 -> "/api/inventory/" + skuId;
            default -> "/api/prices/" + skuId;
        };
        return sendAsync(started, request(path, sequence).GET());
    }

    private CompletionStage<RequestResult> sendPaymentCallback(long sequence) {
        long started = System.nanoTime();
        long paymentId = options.paymentIdBase() + sequence;
        String tradeNo = options.paymentTradeNoPrefix() + sequence;
        Instant timestamp = Instant.now();
        String nonce = requestId("callback", sequence);
        String signature = signPaymentCallback(options.paymentChannel(), tradeNo, paymentId, options.salePrice(),
                timestamp, nonce);
        return sendAsync(started,
                request("/api/payments/" + paymentId + "/callbacks", sequence).POST(jsonBody(Map.of("channel",
                        options.paymentChannel(), "channelTradeNo", tradeNo, "paidAmount", options.salePrice(),
                        "timestamp", timestamp.toString(), "nonce", nonce, "signature", signature))));
    }

    private CompletionStage<RequestResult> sendProductChange(long sequence) {
        long started = System.nanoTime();
        long skuId = trafficModel.skuId(sequence, false);
        return sendAsync(started, request("/api/products/" + skuId + "/title", sequence).method("PATCH",
                jsonBody(Map.of("title", "loadtest product title " + sequence))));
    }

    private CompletionStage<RequestResult> sendFlashSaleHotspot(long sequence) {
        long started = System.nanoTime();
        IdentityFixtureSource.Credential credential = identityFixtures.next(sequence);
        return sendAsync(started,
                request("/api/flash-sales/campaigns/" + options.flashSaleCampaignId() + "/tokens", sequence,
                        credential.token()).header("X-Client-Channel", "flash-sale-hotspot")
                        .POST(jsonBody(Map.of("userId", credential.userId(), "quantity", options.quantity()))));
    }

    private CompletionStage<RequestResult> sendAsync(long started, HttpRequest.Builder request) {
        return httpClient.sendAsync(request.build(), HttpResponse.BodyHandlers.discarding())
                .handle((response, error) -> toResult(started, response, error));
    }

    private HttpRequest.Builder request(String path, long sequence) {
        return request(path, sequence, options.authToken());
    }

    private HttpRequest.Builder request(String path, long sequence, String authToken) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(options.baseUrl() + path))
                .timeout(options.requestTimeout()).header("Content-Type", "application/json")
                .header("X-Device-Id", trafficModel.deviceId(sequence)).header("X-Loadtest-Run-Id", options.runId())
                .header("X-Loadtest-Worker", Integer.toString(options.worker().index()));
        if (authToken != null && !authToken.isBlank()) {
            builder.header("Authorization", "Bearer " + authToken);
        }
        return builder;
    }

    private HttpRequest.BodyPublisher jsonBody(Object body) {
        try {
            return HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(body));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize request body", ex);
        }
    }

    private void post(String path, Object body) {
        try {
            HttpRequest request = request(path, 0L).POST(jsonBody(body)).build();
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

    private RequestResult toResult(long started, HttpResponse<?> response, Throwable error) {
        long elapsedMicros = elapsedMicros(started);
        if (error != null || response == null) {
            return RequestResult.failed(elapsedMicros, errorKind(error));
        }
        return new RequestResult(isSuccess(response.statusCode()), elapsedMicros, response.statusCode(), "");
    }

    String signPaymentCallback(String channel, String tradeNo, long paymentId, java.math.BigDecimal amount,
            Instant timestamp, String nonce) {
        String payload = channel + "\n" + tradeNo + "\n" + paymentId + "\n"
                + amount.stripTrailingZeros().toPlainString() + "\n" + timestamp + "\n" + nonce;
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(options.paymentCallbackSecret().getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("failed to sign payment callback", ex);
        }
    }

    private String requestId(String type, long sequence) {
        return "loadtest-" + options.runId() + '-' + options.worker().index() + '-' + type + '-' + sequence;
    }

    private long elapsedMicros(long startedNanos) {
        return Math.max(1L, TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - startedNanos));
    }

    private boolean isSuccess(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String errorKind(Throwable error) {
        if (error == null) {
            return "transport-error";
        }
        Throwable cause = error;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getClass().getSimpleName();
    }

    @Override
    public void close() {
        identityFixtures.close();
    }
}
