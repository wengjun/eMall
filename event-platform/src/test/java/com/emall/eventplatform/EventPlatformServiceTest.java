package com.emall.eventplatform;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.common.privacy.SensitiveDataType;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class EventPlatformServiceTest {
    private final InMemoryEventPlatformRepository repository = new InMemoryEventPlatformRepository();
    private final EventPlatformService service = new EventPlatformService(repository, new SnowflakeIdGenerator(51L));

    @Test
    void registersSchemaIngestsEventsAndMaterializesMetrics() {
        service.registerSchema("product_view", 1, "growth", "{\"type\":\"object\"}");
        service.activateSchema("product_view", 1);
        service.classifyField("product_view", 1, "mobile", SensitiveDataType.MOBILE, false, false);
        TrackingEvent event = service.ingestEvent("product_view", 1, "event-1", "user-1",
                "{\"skuId\":1001,\"mobile\":\"13800000000\"}", Instant.now());
        service.ingestEvent("product_view", 1, "event-2", "user-2", "{\"skuId\":1002}",
                Instant.now().minusSeconds(90_000));
        MetricMaterialization materialization =
                service.materializeMetric("product_view", "product_view_count", "2026-04-29T14");

        assertThat(event.payload()).contains("138****0000");
        assertThat(event.payload()).doesNotContain("13800000000");
        assertThat(materialization.eventCount()).isEqualTo(2);
        assertThat(materialization.lateEventCount()).isEqualTo(1);
        assertThat(service.summary().activeSchemas()).isEqualTo(1);
        assertThat(service.summary().classifiedFields()).isEqualTo(1);
    }
}
