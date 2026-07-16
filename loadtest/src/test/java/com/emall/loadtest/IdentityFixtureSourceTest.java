package com.emall.loadtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IdentityFixtureSourceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void shouldStreamWorkerSpecificCredentialsWithoutLoadingTheDataset() throws Exception {
        Path fixture = temporaryDirectory.resolve("worker-2.csv");
        Files.writeString(fixture, "userId,token\n# generated fixture\n101,token-a\n102,token-b\n");
        Map<String, String> environment = LoadTestOptionsTest.environment();
        environment.put("EMALL_LOAD_WORKER_INDEX", "2");
        environment.put("EMALL_LOAD_WORKER_COUNT", "3");
        environment.put("EMALL_LOAD_IDENTITY_FIXTURE_FILE",
                temporaryDirectory.resolve("worker-{worker}.csv").toString());
        LoadTestOptions options = LoadTestOptions.from(new String[0], environment);

        try (IdentityFixtureSource source = new IdentityFixtureSource(options, new TrafficModel(options))) {
            assertThat(source.next(3L)).isEqualTo(new IdentityFixtureSource.Credential(101L, "token-a"));
            assertThat(source.next(6L)).isEqualTo(new IdentityFixtureSource.Credential(102L, "token-b"));
            assertThatThrownBy(() -> source.next(9L)).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("exhausted");
        }
    }
}
