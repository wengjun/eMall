package com.emall.loadtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LoadTestOptionsTest {
    @Test
    void shouldParseCliOptionsAndDistributedWorkerIdentity() {
        Map<String, String> environment = environment();
        environment.put("EMALL_LOAD_ROLE", "worker");
        environment.put("EMALL_LOAD_WORKER_INDEX", "2");
        environment.put("EMALL_LOAD_WORKER_COUNT", "4");
        environment.put("EMALL_LOAD_PATTERN", "spike");

        LoadTestOptions options = LoadTestOptions
                .from(new String[]{"http://localhost:8080/", "11", "5", "3", "production-mix"}, environment);

        assertThat(options.baseUrl()).isEqualTo("http://localhost:8080");
        assertThat(options.ratePerSecond()).isEqualTo(11);
        assertThat(options.duration()).isEqualTo(Duration.ofSeconds(5));
        assertThat(options.maxInflight()).isEqualTo(3);
        assertThat(options.scenario()).isEqualTo(LoadScenario.PRODUCTION_MIX);
        assertThat(options.pattern()).isEqualTo(LoadPattern.SPIKE);
        assertThat(options.localRate(11)).isEqualTo(3);
        assertThat(options.worker().globalSequence(3)).isEqualTo(11L);
    }

    @Test
    void shouldRejectBootstrapInDistributedWorkers() {
        Map<String, String> environment = environment();
        environment.put("EMALL_LOAD_ROLE", "worker");
        environment.put("EMALL_LOAD_BOOTSTRAP_DATA", "true");

        assertThatThrownBy(() -> LoadTestOptions.from(new String[0], environment))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("pre-seeded data");
    }

    @Test
    void shouldRejectInvalidWorkerIndex() {
        Map<String, String> environment = environment();
        environment.put("EMALL_LOAD_ROLE", "worker");
        environment.put("EMALL_LOAD_WORKER_INDEX", "4");
        environment.put("EMALL_LOAD_WORKER_COUNT", "4");

        assertThatThrownBy(() -> LoadTestOptions.from(new String[0], environment))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("worker index");
    }

    static Map<String, String> environment() {
        Map<String, String> environment = new HashMap<>();
        environment.put("EMALL_LOAD_BOOTSTRAP_DATA", "false");
        environment.put("EMALL_LOAD_RUN_ID", "test-run");
        environment.put("EMALL_LOAD_DURATION_MS", "100");
        environment.put("EMALL_LOAD_MAX_GENERATOR_CPU", "1.0");
        return environment;
    }
}
