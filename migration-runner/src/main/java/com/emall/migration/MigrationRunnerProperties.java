package com.emall.migration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@Getter
@Setter
@ConfigurationProperties("emall.migration")
public class MigrationRunnerProperties {
    public static final List<String> SUPPORTED_SERVICES = List.of("advertising", "after-sales", "analytics", "cart",
            "catalog", "cost", "customer-service", "data-warehouse", "event-platform", "experiment", "finance",
            "flash-sale", "forecasting", "fulfillment", "identity", "intelligence", "inventory", "marketing",
            "merchant", "openapi", "operations", "order", "payment", "platform-ops", "pricing", "product", "promotion",
            "recommendation", "release", "reliability", "review", "risk", "routing", "search", "supply-chain",
            "traffic", "user");

    private String service = "";
    private String region = "default";
    private List<Integer> shards = new ArrayList<>(List.of(0));
    private String jdbcUrlTemplate = "";
    private String username = "";
    private String password = "";
    private List<String> locations = new ArrayList<>(List.of("filesystem:/app/migrations"));
    private String historyTable = "flyway_schema_history";
    private String operator = "unknown";
    private String batchId = "unknown";
    private boolean baselineOnMigrate;
    private boolean dryRun;
    private boolean createPhysicalTables;
    private int defaultTableShardCount = 64;
    private String cellId = "cell-a";
    private int canaryShardCount = 1;
    private int batchSize = 4;
    private int maxParallelism = 2;
    private Duration batchTimeout = Duration.ofMinutes(15);
    private Duration pauseBetweenBatches = Duration.ZERO;
    private MigrationPhase phase = MigrationPhase.EXPAND;
    private boolean allowDestructiveChanges;
    private String minimumCompatibleVersion = "";
    private String approvalReference = "";

    public List<MigrationTarget> expandTargets() {
        String normalizedService = normalizeService();
        List<Integer> normalizedShards = normalizeShards();
        boolean suffixedDatabase =
                normalizedShards.size() > 1 || normalizedShards.stream().anyMatch(shard -> shard > 0);
        List<MigrationTarget> targets = new ArrayList<>(normalizedShards.size());
        for (Integer shard : normalizedShards) {
            targets.add(target(normalizedService, shard, suffixedDatabase));
        }
        return List.copyOf(targets);
    }

    public List<List<MigrationTarget>> planBatches() {
        return MigrationBatchPlanner.plan(expandTargets(), canaryShardCount, batchSize);
    }

    private MigrationTarget target(String normalizedService, int shard, boolean suffixedDatabase) {
        String shardText = "%02d".formatted(shard);
        String databasePrefix = "emall_%s".formatted(normalizedService.replace('-', '_'));
        String database = suffixedDatabase ? databasePrefix + '_' + shardText : databasePrefix;
        String jdbcUrl = replaceTokens(jdbcUrlTemplate, normalizedService, shardText, database);
        validateDatabaseScope(jdbcUrl, databasePrefix);
        if (!dryRun && (!StringUtils.hasText(username) || !StringUtils.hasText(password))) {
            throw new IllegalStateException("service-scoped migration username and password must be configured");
        }
        List<String> resolvedLocations = locations == null
                ? List.of()
                : locations.stream().map(String::trim).filter(StringUtils::hasText).toList();
        if (resolvedLocations.isEmpty()) {
            throw new IllegalStateException("emall.migration.locations must contain the service migration artifact");
        }
        return new MigrationTarget(normalizedService, region.trim(), shard, database, jdbcUrl, username, password,
                resolvedLocations, historyTable, operator, batchId, baselineOnMigrate, dryRun, createPhysicalTables,
                defaultPhysicalTables(normalizedService), phase, allowDestructiveChanges, minimumCompatibleVersion,
                approvalReference);
    }

    private String normalizeService() {
        String normalized = service == null ? "" : service.trim();
        if (!SUPPORTED_SERVICES.contains(normalized)) {
            throw new IllegalStateException(
                    "emall.migration.service must identify one supported service: " + normalized);
        }
        if (!StringUtils.hasText(region)) {
            throw new IllegalStateException("emall.migration.region must be configured");
        }
        if (!StringUtils.hasText(jdbcUrlTemplate)) {
            throw new IllegalStateException("emall.migration.jdbc-url-template must be configured");
        }
        return normalized;
    }

    private List<Integer> normalizeShards() {
        if (shards == null || shards.isEmpty()) {
            throw new IllegalStateException("emall.migration.shards must contain at least one shard");
        }
        if (shards.stream().anyMatch(shard -> shard == null || shard < 0)) {
            throw new IllegalStateException("emall.migration.shards must contain non-negative shard indexes");
        }
        List<Integer> normalized = shards.stream().distinct().sorted().toList();
        if (canaryShardCount <= 0 || canaryShardCount > normalized.size()) {
            throw new IllegalStateException(
                    "emall.migration.canary-shard-count must be between one and the shard count");
        }
        if (batchSize <= 0 || maxParallelism <= 0) {
            throw new IllegalStateException("migration batch size and parallelism must be positive");
        }
        if (batchTimeout == null || batchTimeout.isZero() || batchTimeout.isNegative()) {
            throw new IllegalStateException("emall.migration.batch-timeout must be positive");
        }
        if (pauseBetweenBatches == null || pauseBetweenBatches.isNegative()) {
            throw new IllegalStateException("emall.migration.pause-between-batches cannot be negative");
        }
        return normalized;
    }

    private void validateDatabaseScope(String jdbcUrl, String databasePrefix) {
        if (jdbcUrl.toLowerCase().contains("createdatabaseifnotexist=true")) {
            throw new IllegalStateException("migration jobs cannot create databases");
        }
        int scheme = jdbcUrl.indexOf("://");
        int pathStart = scheme < 0 ? -1 : jdbcUrl.indexOf('/', scheme + 3);
        int queryStart = pathStart < 0 ? -1 : jdbcUrl.indexOf('?', pathStart + 1);
        String database =
                pathStart < 0 ? "" : jdbcUrl.substring(pathStart + 1, queryStart < 0 ? jdbcUrl.length() : queryStart);
        boolean serviceDatabase = database.equals(databasePrefix)
                || database.matches(java.util.regex.Pattern.quote(databasePrefix) + "_[0-9]+");
        if (!serviceDatabase) {
            throw new IllegalStateException("migration JDBC URL escapes service database boundary: " + database);
        }
    }

    private List<PhysicalTableRule> defaultPhysicalTables(String normalizedService) {
        return switch (normalizedService) {
            case "order" -> List.of(rule("order_record"), rule("outbox_event"), rule("order_create_saga"),
                    rule("order_payment_confirmation"));
            case "payment" -> List.of(rule("payment_order"), rule("payment_ledger_entry"),
                    rule("payment_channel_statement"), rule("payment_reconciliation_record"));
            case "inventory" ->
                List.of(rule("inventory_item"), rule("inventory_bucket"), rule("inventory_reservation"));
            case "product" -> List.of(rule("product"), rule("outbox_event"));
            case "pricing" -> List.of(rule("price_book"));
            case "search" -> List.of(rule("search_document"), rule("processed_message"));
            case "user" -> List.of(rule("user_account"));
            case "cart" -> List.of(rule("cart_item"));
            case "marketing" -> List.of(rule("coupon"));
            case "flash-sale" -> List.of(rule("flash_sale_campaign"), rule("flash_sale_stock"),
                    rule("flash_sale_token"), rule("flash_sale_order_request"));
            default -> List.of();
        };
    }

    private PhysicalTableRule rule(String table) {
        return new PhysicalTableRule(table, table, defaultTableShardCount, cellId);
    }

    private String replaceTokens(String value, String normalizedService, String shard, String database) {
        return value.replace("{service}", normalizedService).replace("{region}", region.trim())
                .replace("{shard}", shard).replace("{database}", database);
    }

}
