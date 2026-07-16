package com.emall.loadtest;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

record LoadTestOptions(String baseUrl, int ratePerSecond, Duration duration, int maxInflight, LoadScenario scenario,
        LoadPattern pattern, LoadRole role, WorkerIdentity worker, String runId, Path reportDirectory,
        Duration backpressureTimeout, Duration requestTimeout, long userId, long skuId, int quantity,
        double maxErrorRate, long maxP95Micros, boolean bootstrapData, int bootstrapStock, BigDecimal listPrice,
        BigDecimal salePrice, String currency, String keyword, String paymentChannel, long paymentIdBase,
        String paymentTradeNoPrefix, String paymentCallbackSecret, long flashSaleCampaignId, int userCardinality,
        int skuCardinality, int hotSkuPercent, String trafficMix, Duration maxSchedulerLag, double maxGeneratorCpu,
        String environment, String deployment, String targetResources, long datasetUsers, long datasetSkus,
        int serviceInstances, String gitCommit, Path saturationFile, int cellCount, double scalingEfficiency,
        double capacityHeadroom, long targetPeakConcurrency, double sessionThinkSeconds, String evidenceScope,
        int requiredRuns, List<String> evidenceRunIds, boolean requireVerifiedEvidence, String authToken,
        Path identityFixtureFile, String faultExperiment) {

    LoadTestOptions {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("base URL must not be blank");
        }
        if (ratePerSecond <= 0 || duration.isZero() || duration.isNegative() || maxInflight <= 0) {
            throw new IllegalArgumentException("rate, duration, and max in-flight requests must be positive");
        }
        if (maxErrorRate < 0.0 || maxErrorRate >= 1.0 || maxP95Micros <= 0) {
            throw new IllegalArgumentException("invalid error-rate or latency threshold");
        }
        if (userCardinality <= 0 || skuCardinality <= 0 || hotSkuPercent < 0 || hotSkuPercent > 100) {
            throw new IllegalArgumentException("invalid traffic data model");
        }
        if (scalingEfficiency <= 0.0 || scalingEfficiency > 1.0 || capacityHeadroom <= 0.0 || capacityHeadroom > 1.0
                || cellCount <= 0 || sessionThinkSeconds <= 0.0) {
            throw new IllegalArgumentException("invalid capacity model");
        }
        if (maxGeneratorCpu <= 0.0 || maxGeneratorCpu > 1.0 || requiredRuns <= 0 || serviceInstances <= 0
                || datasetUsers <= 0L || datasetSkus <= 0L || targetPeakConcurrency <= 0L) {
            throw new IllegalArgumentException("invalid evidence or load-generator limits");
        }
        if (role == LoadRole.WORKER && bootstrapData) {
            throw new IllegalArgumentException("distributed workers require pre-seeded data; disable bootstrap data");
        }
        runId = sanitizeRunId(runId);
        evidenceRunIds = List.copyOf(evidenceRunIds);
    }

    static LoadTestOptions from(String[] args) {
        return from(args, System.getenv());
    }

    static LoadTestOptions from(String[] args, Map<String, String> environment) {
        LoadRole role = LoadRole.from(env(environment, "EMALL_LOAD_ROLE", "standalone"));
        int workerCount = integer(environment, "EMALL_LOAD_WORKER_COUNT", "1");
        int workerIndex =
                integer(environment, "EMALL_LOAD_WORKER_INDEX", env(environment, "JOB_COMPLETION_INDEX", "0"));
        LoadScenario scenario = LoadScenario.from(value(args, 4, env(environment, "EMALL_LOAD_SCENARIO", "checkout")));
        int userCardinality = integer(environment, "EMALL_LOAD_USER_CARDINALITY", "100000");
        int skuCardinality = integer(environment, "EMALL_LOAD_SKU_CARDINALITY", "1");
        String configuredRunId = environment.get("EMALL_LOAD_RUN_ID");
        String runId = configuredRunId == null || configuredRunId.isBlank()
                ? role == LoadRole.COORDINATOR ? "all" : UUID.randomUUID().toString()
                : configuredRunId;

        return new LoadTestOptions(
                trimTrailingSlash(value(args, 0, env(environment, "EMALL_BASE_URL", "http://localhost:8080"))),
                integer(args, 1, env(environment, "EMALL_LOAD_RATE", "100")), duration(args, environment),
                integer(args, 3,
                        env(environment, "EMALL_LOAD_MAX_INFLIGHT",
                                env(environment, "EMALL_LOAD_MAX_CONCURRENCY", "200"))),
                scenario, LoadPattern.from(env(environment, "EMALL_LOAD_PATTERN", "constant")), role,
                new WorkerIdentity(workerIndex, workerCount), runId,
                Path.of(env(environment, "EMALL_LOAD_REPORT_DIR", "target/loadtest-reports")),
                Duration.ofMillis(longValue(environment, "EMALL_LOAD_BACKPRESSURE_TIMEOUT_MS", "100")),
                Duration.ofMillis(longValue(environment, "EMALL_LOAD_REQUEST_TIMEOUT_MS", "10000")),
                longValue(environment, "EMALL_LOAD_USER_ID", "100001"),
                longValue(environment, "EMALL_LOAD_SKU_ID", "10001"), integer(environment, "EMALL_LOAD_QUANTITY", "1"),
                decimal(environment, "EMALL_LOAD_MAX_ERROR_RATE", "0.01"),
                Math.multiplyExact(longValue(environment, "EMALL_LOAD_MAX_P95_MS", "500"), 1_000L),
                Boolean.parseBoolean(env(environment, "EMALL_LOAD_BOOTSTRAP_DATA", "true")),
                integer(environment, "EMALL_LOAD_BOOTSTRAP_STOCK", "1000000"),
                new BigDecimal(env(environment, "EMALL_LOAD_LIST_PRICE", "3999.00")),
                new BigDecimal(env(environment, "EMALL_LOAD_SALE_PRICE", "3799.00")),
                env(environment, "EMALL_LOAD_CURRENCY", "CNY"), env(environment, "EMALL_LOAD_KEYWORD", "phone"),
                env(environment, "EMALL_LOAD_PAYMENT_CHANNEL", "loadtest"),
                longValue(environment, "EMALL_LOAD_PAYMENT_ID_BASE", "800000000"),
                env(environment, "EMALL_LOAD_PAYMENT_TRADE_NO_PREFIX", "loadtest-trade-"),
                env(environment, "EMALL_LOAD_PAYMENT_CALLBACK_SECRET", ""),
                longValue(environment, "EMALL_FLASH_SALE_CAMPAIGN_ID", "90001"), userCardinality, skuCardinality,
                integer(environment, "EMALL_LOAD_HOT_SKU_PERCENT", "20"),
                env(environment, "EMALL_LOAD_TRAFFIC_MIX",
                        "read-heavy:65,checkout:15,hot-sku:10,payment-callbacks:5,mq-backlog:3,flash-sale-hotspot:2"),
                Duration.ofMillis(longValue(environment, "EMALL_LOAD_MAX_SCHEDULER_LAG_MS", "250")),
                decimal(environment, "EMALL_LOAD_MAX_GENERATOR_CPU", "0.85"),
                env(environment, "EMALL_LOAD_ENVIRONMENT", "local"),
                env(environment, "EMALL_LOAD_DEPLOYMENT", "standalone"),
                env(environment, "EMALL_LOAD_TARGET_RESOURCES", "unspecified"),
                longValue(environment, "EMALL_LOAD_DATASET_USERS", Integer.toString(userCardinality)),
                longValue(environment, "EMALL_LOAD_DATASET_SKUS", Integer.toString(skuCardinality)),
                integer(environment, "EMALL_LOAD_SERVICE_INSTANCES", "1"),
                env(environment, "EMALL_LOAD_GIT_COMMIT", "unknown"),
                optionalPath(environment, "EMALL_LOAD_SATURATION_FILE"),
                integer(environment, "EMALL_LOAD_CELL_COUNT", "1"),
                decimal(environment, "EMALL_LOAD_SCALING_EFFICIENCY", "0.80"),
                decimal(environment, "EMALL_LOAD_CAPACITY_HEADROOM", "0.70"),
                longValue(environment, "EMALL_LOAD_TARGET_PEAK_CONCURRENCY", "1000000"),
                decimal(environment, "EMALL_LOAD_SESSION_THINK_SECONDS", "30"),
                env(environment, "EMALL_LOAD_EVIDENCE_SCOPE", "development"),
                integer(environment, "EMALL_LOAD_REQUIRED_RUNS", "3"),
                csv(environment.get("EMALL_LOAD_EVIDENCE_RUN_IDS")),
                Boolean.parseBoolean(env(environment, "EMALL_LOAD_REQUIRE_VERIFIED_EVIDENCE", "false")),
                env(environment, "EMALL_LOAD_AUTH_TOKEN", ""),
                optionalWorkerPath(environment, "EMALL_LOAD_IDENTITY_FIXTURE_FILE", workerIndex),
                env(environment, "EMALL_LOAD_FAULT_EXPERIMENT", ""));
    }

    int localRate(int globalRate) {
        return worker.localRate(globalRate);
    }

    private static String value(String[] args, int index, String defaultValue) {
        return args.length > index && !args[index].isBlank() ? args[index] : defaultValue;
    }

    private static int integer(String[] args, int index, String defaultValue) {
        return Integer.parseInt(value(args, index, defaultValue));
    }

    private static Duration duration(String[] args, Map<String, String> environment) {
        if (args.length > 2 && !args[2].isBlank()) {
            return Duration.ofSeconds(Long.parseLong(args[2]));
        }
        String durationMillis = environment.get("EMALL_LOAD_DURATION_MS");
        return durationMillis == null || durationMillis.isBlank()
                ? Duration.ofSeconds(longValue(environment, "EMALL_LOAD_DURATION_SECONDS", "60"))
                : Duration.ofMillis(Long.parseLong(durationMillis));
    }

    private static int integer(Map<String, String> values, String key, String defaultValue) {
        return Integer.parseInt(env(values, key, defaultValue));
    }

    private static long longValue(Map<String, String> values, String key, String defaultValue) {
        return Long.parseLong(env(values, key, defaultValue));
    }

    private static double decimal(Map<String, String> values, String key, String defaultValue) {
        return Double.parseDouble(env(values, key, defaultValue));
    }

    private static String env(Map<String, String> values, String key, String defaultValue) {
        String value = values.get(key);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static Path optionalPath(Map<String, String> values, String key) {
        String value = values.get(key);
        return value == null || value.isBlank() ? null : Path.of(value.trim());
    }

    private static Path optionalWorkerPath(Map<String, String> values, String key, int workerIndex) {
        String value = values.get(key);
        return value == null || value.isBlank()
                ? null
                : Path.of(value.trim().replace("{worker}", Integer.toString(workerIndex)));
    }

    private static List<String> csv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(",")).map(String::trim).filter(item -> !item.isBlank()).toList();
    }

    private static String trimTrailingSlash(String value) {
        String normalized = value.trim();
        while (normalized.endsWith("/") && normalized.length() > "https://a".length()) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String sanitizeRunId(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank() || !normalized.matches("[a-z0-9._-]+")) {
            throw new IllegalArgumentException("run ID may contain only letters, digits, '.', '_' and '-'");
        }
        return normalized;
    }
}
