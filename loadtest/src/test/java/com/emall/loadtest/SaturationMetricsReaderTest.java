package com.emall.loadtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SaturationMetricsReaderTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void shouldReadValidatedFlatMetrics() throws Exception {
        Path metrics = temporaryDirectory.resolve("saturation.json");
        Files.writeString(metrics, "{\"gateway.cpu.utilization\":0.7,\"kafka.consumer.lag\":42}");

        assertThat(SaturationMetricsReader.read(metrics)).containsEntry("gateway.cpu.utilization", 0.7)
                .containsEntry("kafka.consumer.lag", 42.0);
    }

    @Test
    void shouldRejectOutOfRangeUtilization() throws Exception {
        Path metrics = temporaryDirectory.resolve("invalid.json");
        Files.writeString(metrics, "{\"generator.network.utilization\":1.2}");

        assertThatThrownBy(() -> SaturationMetricsReader.read(metrics)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("[0, 1]");
    }
}
