package com.emall.intelligence;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.common.id.SnowflakeIdGenerator;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class IntelligenceServiceTest {
    private final InMemoryIntelligenceRepository repository = new InMemoryIntelligenceRepository();
    private final IntelligenceService service = new IntelligenceService(repository, new SnowflakeIdGenerator(53L));

    @Test
    void managesProfilesFeaturesModelsAndDecisions() {
        service.upsertUserProfile(1001L, "premium", "electronics", false);
        service.upsertItemProfile(2001L, "phone", "memory=256", new BigDecimal("0.95"));
        service.registerFeature("user_order_30d", FeatureScope.USER, "risk", 300);
        service.writeFeatureValue("user_order_30d", "1001", "12", Instant.now());
        ModelDeployment model = service.registerModel("fraud-score", "v1", "fraud");
        service.changeModelStatus(model.modelId(), ModelStatus.DEPLOYED, "approval-1");
        service.recordDecision("fraud", "order-1", "pass", new BigDecimal("0.100000"), "v1");

        IntelligenceSummary summary = service.summary();

        assertThat(summary.userProfiles()).isEqualTo(1);
        assertThat(summary.itemProfiles()).isEqualTo(1);
        assertThat(summary.deployedModels()).isEqualTo(1);
        assertThat(summary.decisions()).isEqualTo(1);
    }
}
