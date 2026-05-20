package com.emall.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DataPlatformContractIT {
    @Test
    void shouldExposeEventPlatformAndWarehouseContractsAgainstRunningServices() throws Exception {
        ProductionHttpGate.assumeEnabled("EMALL_RUN_DATA_PLATFORM_IT");
        String eventBaseUrl = ProductionHttpGate.envOrDefault("EMALL_EVENT_PLATFORM_URL", "http://localhost:8109");
        String warehouseBaseUrl = ProductionHttpGate.envOrDefault("EMALL_DATA_WAREHOUSE_URL", "http://localhost:8110");
        String suffix = String.valueOf(System.currentTimeMillis());
        String eventName = "order_paid_" + suffix;

        ProductionHttpGate.postJson(eventBaseUrl, "/api/event-platform/schemas",
                Map.of("eventName", eventName, "version", 1, "owner", "growth", "jsonSchema", "{\"type\":\"object\"}"),
                null);
        JsonNode activeSchema = ProductionHttpGate.patchJson(eventBaseUrl, "/api/event-platform/schemas/activate",
                Map.of("eventName", eventName, "version", 1), null);
        ProductionHttpGate.postJson(
                eventBaseUrl, "/api/event-platform/field-classifications", Map.of("eventName", eventName, "version", 1,
                        "fieldName", "mobile", "sensitivity", "MOBILE", "required", true, "exportedToWarehouse", true),
                null);
        ProductionHttpGate.postJson(eventBaseUrl, "/api/event-platform/events",
                Map.of("eventName", eventName, "version", 1, "eventKey", "event-" + suffix, "userKey", "user-" + suffix,
                        "payload", "{\"mobile\":\"15500000000\"}", "occurredAt", Instant.now()),
                null);
        ProductionHttpGate.postJson(eventBaseUrl, "/api/event-platform/offsets",
                Map.of("consumerGroup", "warehouse-loader", "topicName", eventName, "processedOffset", 100L), null);
        ProductionHttpGate.postJson(eventBaseUrl, "/api/event-platform/materializations",
                Map.of("eventName", eventName, "metricName", "paid_orders", "windowKey", "m5-" + suffix), null);

        assertThat(activeSchema.path("data").path("status").asText()).isEqualTo("ACTIVE");
        JsonNode eventSummary = ProductionHttpGate.getJson(eventBaseUrl, "/api/event-platform/summary");
        assertThat(eventSummary.path("data").path("activeSchemas").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(eventSummary.path("data").path("ingestedEvents").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(eventSummary.path("data").path("materializedMetrics").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(eventSummary.path("data").path("classifiedFields").asInt()).isGreaterThanOrEqualTo(1);

        JsonNode ods = ProductionHttpGate.postJson(warehouseBaseUrl, "/api/data-warehouse/datasets",
                Map.of("layer", "ODS", "datasetName", "ods_order_paid_" + suffix, "owner", "growth", "description",
                        "order paid raw events", "retentionDays", 30),
                null);
        JsonNode dwd = ProductionHttpGate.postJson(warehouseBaseUrl, "/api/data-warehouse/datasets",
                Map.of("layer", "DWD", "datasetName", "dwd_order_paid_" + suffix, "owner", "growth", "description",
                        "order paid cleaned facts", "retentionDays", 365),
                null);
        long odsId = ods.path("data").path("datasetId").asLong();
        long dwdId = dwd.path("data").path("datasetId").asLong();

        ProductionHttpGate
                .postJson(
                        warehouseBaseUrl, "/api/data-warehouse/partitions", Map.of("datasetId", odsId, "partitionKey",
                                "dt", "partitionDate", LocalDate.now(), "rowCount", 1000L, "storageBytes", 4096L),
                        null);
        ProductionHttpGate.postJson(warehouseBaseUrl, "/api/data-warehouse/quality-checks", Map.of("datasetId", odsId,
                "checkName", "not_null_order_id", "status", "FAIL", "detail", "missing order id"), null);
        ProductionHttpGate.postJson(warehouseBaseUrl, "/api/data-warehouse/lineage",
                Map.of("upstreamDatasetId", odsId, "downstreamDatasetId", dwdId, "transformName", "clean_order_paid"),
                null);
        ProductionHttpGate.postJson(warehouseBaseUrl, "/api/data-warehouse/field-lineage",
                Map.of("upstreamDatasetId", odsId, "upstreamField", "mobile", "downstreamDatasetId", dwdId,
                        "downstreamField", "masked_mobile", "sensitivity", "MOBILE", "transformName", "mask_mobile"),
                null);

        JsonNode warehouseSummary = ProductionHttpGate.getJson(warehouseBaseUrl, "/api/data-warehouse/summary");
        assertThat(warehouseSummary.path("data").path("datasets").asInt()).isGreaterThanOrEqualTo(2);
        assertThat(warehouseSummary.path("data").path("partitions").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(warehouseSummary.path("data").path("failedChecks").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(warehouseSummary.path("data").path("lineageEdges").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(warehouseSummary.path("data").path("fieldLineageEdges").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(warehouseSummary.path("data").path("openQualityAlerts").asInt()).isGreaterThanOrEqualTo(1);
    }
}
