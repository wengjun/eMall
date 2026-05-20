package com.emall.migration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties("emall.migration")
public class MigrationRunnerProperties {
    private boolean dryRun;
    private boolean baselineOnMigrate = true;
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
    private List<Integer> shards = List.of(0);
    private List<String> locations = List.of("filesystem:/migrations/{service}");
    private Map<String, String> serviceLocations = new LinkedHashMap<>();

    public List<MigrationTarget> expandTargets() {
        List<String> normalizedServices = nonBlank(services);
        if (normalizedServices.isEmpty()) {
            throw new IllegalStateException("emall.migration.services must contain at least one service");
        }
        if (!StringUtils.hasText(jdbcUrlTemplate)) {
            throw new IllegalStateException("emall.migration.jdbc-url-template must be configured");
        }
        List<String> normalizedRegions = nonBlank(regions);
        List<Integer> normalizedShards = shards == null || shards.isEmpty() ? List.of(0) : shards;
        List<MigrationTarget> targets = new ArrayList<>();
        for (String service : normalizedServices) {
            for (String region : normalizedRegions) {
                for (Integer shard : normalizedShards) {
                    targets.add(target(service, region, shard == null ? 0 : shard));
                }
            }
        }
        return targets;
    }

    private MigrationTarget target(String service, String region, int shard) {
        String shardText = Integer.toString(shard);
        String jdbcUrl = replaceTokens(jdbcUrlTemplate, service, region, shardText);
        List<String> targetLocations =
                serviceLocations.containsKey(service) ? List.of(serviceLocations.get(service).split(",")) : locations;
        return new MigrationTarget(service, region, shard, jdbcUrl, username, password,
                targetLocations.stream().map(location -> replaceTokens(location.trim(), service, region, shardText))
                        .filter(StringUtils::hasText).toList(),
                historyTable, operator, baselineOnMigrate, dryRun, createPhysicalTables,
                defaultPhysicalTables(service));
    }

    private List<PhysicalTableRule> defaultPhysicalTables(String service) {
        String normalized = service.trim();
        return switch (normalized) {
            case "order" -> List.of(rule("order_record"), rule("outbox_event"));
            case "payment" ->
                List.of(rule("payment_order"), rule("payment_ledger_entry"), rule("payment_channel_statement"),
                        rule("payment_reconciliation_record"), rule("payment_refund_order"));
            case "inventory" ->
                List.of(rule("inventory_item"), rule("inventory_bucket"), rule("inventory_reservation"));
            case "product" -> List.of(rule("product"), rule("outbox_event"));
            case "pricing" -> List.of(rule("price_book"));
            case "search" -> List.of(rule("search_document"), rule("processed_message"));
            case "user" -> List.of(rule("user_account"));
            case "cart" -> List.of(rule("cart_item"));
            case "flash-sale" -> List.of(rule("flash_sale_campaign"), rule("flash_sale_stock"),
                    rule("flash_sale_token"), rule("flash_sale_order_request"));
            default -> List.of();
        };
    }

    private PhysicalTableRule rule(String table) {
        return new PhysicalTableRule(table, table, defaultTableShardCount, cellId);
    }

    private String replaceTokens(String value, String service, String region, String shard) {
        return value.replace("{service}", service).replace("{region}", region).replace("{shard}", shard);
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
