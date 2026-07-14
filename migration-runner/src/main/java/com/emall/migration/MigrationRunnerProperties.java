package com.emall.migration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties("emall.migration")
public class MigrationRunnerProperties {
    public static final List<String> SUPPORTED_SERVICES = List.of("advertising", "after-sales", "analytics", "cart",
            "catalog", "cost", "customer-service", "data-warehouse", "event-platform", "experiment", "finance",
            "flash-sale", "forecasting", "fulfillment", "identity", "intelligence", "inventory", "marketing",
            "merchant", "openapi", "operations", "order", "payment", "platform-ops", "pricing", "product", "promotion",
            "recommendation", "release", "reliability", "review", "risk", "search", "supply-chain", "traffic", "user");
    private boolean dryRun;
    private boolean baselineOnMigrate;
    private String operator = "unknown";
    private String jdbcUrlTemplate = "";
    private String username = "";
    private String password = "";
    private String historyTable = "flyway_schema_history";
    private boolean createPhysicalTables;
    private int defaultTableShardCount = 64;
    private String cellId = "cell-a";
    private List<String> services = new ArrayList<>();
    private List<String> regions = List.of("default");
    private List<Integer> shards = List.of();
    private int defaultServiceShardCount = 1;
    private Map<String, Integer> serviceShardCounts = new LinkedHashMap<>();
    private List<String> locations = List.of("classpath:migrations/{service}/src/main/resources/db/migration");
    private Map<String, String> serviceLocations = new LinkedHashMap<>();

    public List<MigrationTarget> expandTargets() {
        List<String> normalizedServices = nonBlank(services);
        if (normalizedServices.isEmpty()) {
            throw new IllegalStateException("emall.migration.services must contain at least one service");
        }
        if (!StringUtils.hasText(jdbcUrlTemplate)) {
            throw new IllegalStateException("emall.migration.jdbc-url-template must be configured");
        }
        List<String> unsupported =
                normalizedServices.stream().filter(service -> !SUPPORTED_SERVICES.contains(service)).toList();
        if (!unsupported.isEmpty()) {
            throw new IllegalStateException("unsupported migration services: " + unsupported);
        }
        List<String> normalizedRegions = nonBlank(regions);
        List<Integer> normalizedShards = shards == null ? List.of() : shards;
        if (normalizedShards.stream().anyMatch(shard -> shard == null || shard < 0)) {
            throw new IllegalStateException("emall.migration.shards must contain non-negative shard indexes");
        }
        List<MigrationTarget> targets = new ArrayList<>();
        for (String service : normalizedServices) {
            for (String region : normalizedRegions) {
                List<Integer> serviceShards = serviceShards(service, normalizedShards);
                boolean suffixedDatabase =
                        serviceShards.size() > 1 || serviceShards.stream().anyMatch(index -> index > 0);
                for (Integer shard : serviceShards) {
                    targets.add(target(service, region, shard == null ? 0 : shard, suffixedDatabase));
                }
            }
        }
        return targets;
    }

    private List<Integer> serviceShards(String service, List<Integer> configuredShards) {
        Integer count = serviceShardCounts.get(service);
        if (count == null && !configuredShards.isEmpty()) {
            return configuredShards;
        }
        int resolvedCount = count == null ? defaultServiceShardCount : count;
        if (resolvedCount <= 0) {
            throw new IllegalStateException("service shard count must be positive for " + service);
        }
        return IntStream.range(0, resolvedCount).boxed().toList();
    }

    private MigrationTarget target(String service, String region, int shard, boolean suffixedDatabase) {
        String shardText = "%02d".formatted(shard);
        String databasePrefix = "emall_%s".formatted(service.replace('-', '_'));
        String database = suffixedDatabase ? databasePrefix + '_' + shardText : databasePrefix;
        String jdbcUrl = replaceTokens(jdbcUrlTemplate, service, region, shardText, database);
        List<String> targetLocations =
                serviceLocations.containsKey(service) ? List.of(serviceLocations.get(service).split(",")) : locations;
        return new MigrationTarget(service, region, shard, jdbcUrl, username, password,
                targetLocations.stream()
                        .map(location -> replaceTokens(location.trim(), service, region, shardText, database))
                        .filter(StringUtils::hasText).toList(),
                historyTable, operator, baselineOnMigrate, dryRun, createPhysicalTables,
                defaultPhysicalTables(service));
    }

    private List<PhysicalTableRule> defaultPhysicalTables(String service) {
        String normalized = service.trim();
        return switch (normalized) {
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

    private String replaceTokens(String value, String service, String region, String shard, String database) {
        return value.replace("{service}", service).replace("{region}", region).replace("{shard}", shard)
                .replace("{database}", database);
    }

    private List<String> nonBlank(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().map(String::trim).filter(StringUtils::hasText).toList();
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public boolean isBaselineOnMigrate() {
        return baselineOnMigrate;
    }

    public void setBaselineOnMigrate(boolean baselineOnMigrate) {
        this.baselineOnMigrate = baselineOnMigrate;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getJdbcUrlTemplate() {
        return jdbcUrlTemplate;
    }

    public void setJdbcUrlTemplate(String jdbcUrlTemplate) {
        this.jdbcUrlTemplate = jdbcUrlTemplate;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getHistoryTable() {
        return historyTable;
    }

    public void setHistoryTable(String historyTable) {
        this.historyTable = historyTable;
    }

    public boolean isCreatePhysicalTables() {
        return createPhysicalTables;
    }

    public void setCreatePhysicalTables(boolean createPhysicalTables) {
        this.createPhysicalTables = createPhysicalTables;
    }

    public int getDefaultTableShardCount() {
        return defaultTableShardCount;
    }

    public void setDefaultTableShardCount(int defaultTableShardCount) {
        this.defaultTableShardCount = defaultTableShardCount;
    }

    public String getCellId() {
        return cellId;
    }

    public void setCellId(String cellId) {
        this.cellId = cellId;
    }

    public List<String> getServices() {
        return services;
    }

    public void setServices(List<String> services) {
        this.services = services;
    }

    public List<String> getRegions() {
        return regions;
    }

    public void setRegions(List<String> regions) {
        this.regions = regions;
    }

    public List<Integer> getShards() {
        return shards;
    }

    public void setShards(List<Integer> shards) {
        this.shards = shards;
    }

    public int getDefaultServiceShardCount() {
        return defaultServiceShardCount;
    }

    public void setDefaultServiceShardCount(int defaultServiceShardCount) {
        this.defaultServiceShardCount = defaultServiceShardCount;
    }

    public Map<String, Integer> getServiceShardCounts() {
        return serviceShardCounts;
    }

    public void setServiceShardCounts(Map<String, Integer> serviceShardCounts) {
        this.serviceShardCounts = new LinkedHashMap<>(serviceShardCounts);
    }

    public List<String> getLocations() {
        return locations;
    }

    public void setLocations(List<String> locations) {
        this.locations = locations;
    }

    public Map<String, String> getServiceLocations() {
        return serviceLocations;
    }

    public void setServiceLocations(Map<String, String> serviceLocations) {
        this.serviceLocations = serviceLocations;
    }
}
