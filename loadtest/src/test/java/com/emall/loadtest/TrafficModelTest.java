package com.emall.loadtest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

class TrafficModelTest {
    @Test
    void shouldApplyDeterministicMixCardinalityAndHotspot() {
        Map<String, String> environment = LoadTestOptionsTest.environment();
        environment.put("EMALL_LOAD_SCENARIO", "production-mix");
        environment.put("EMALL_LOAD_TRAFFIC_MIX", "read-heavy:3,checkout:1");
        environment.put("EMALL_LOAD_USER_CARDINALITY", "20");
        environment.put("EMALL_LOAD_SKU_CARDINALITY", "10");
        environment.put("EMALL_LOAD_HOT_SKU_PERCENT", "100");
        LoadTestOptions options = LoadTestOptions.from(new String[0], environment);
        TrafficModel model = new TrafficModel(options);

        long readRequests = LongStream.rangeClosed(1, 1_000)
                .filter(sequence -> model.scenario(sequence) == LoadScenario.READ_HEAVY).count();

        assertThat(readRequests).isBetween(650L, 850L);
        assertThat(LongStream.rangeClosed(1, 100).map(model::userId).distinct().count()).isLessThanOrEqualTo(20L);
        assertThat(LongStream.rangeClosed(1, 100).map(sequence -> model.skuId(sequence, false)).distinct().toArray())
                .containsExactly(options.skuId());
        assertThat(model.scenario(42L)).isEqualTo(model.scenario(42L));
    }
}
