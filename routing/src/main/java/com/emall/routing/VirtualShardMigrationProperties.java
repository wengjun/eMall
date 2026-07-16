package com.emall.routing;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("emall.routing.virtual-shard-migration")
public class VirtualShardMigrationProperties {
    private int virtualShardCount = 4096;
    private Duration mappingCacheTtl = Duration.ofSeconds(30);
    private Duration minimumCutoverDelay = Duration.ofSeconds(60);
    private Duration observationPeriod = Duration.ofMinutes(10);

    public void validate() {
        if (virtualShardCount <= 0 || Integer.bitCount(virtualShardCount) != 1) {
            throw new IllegalStateException("virtual shard count must be a positive power of two");
        }
        if (mappingCacheTtl == null || mappingCacheTtl.isZero() || mappingCacheTtl.isNegative()) {
            throw new IllegalStateException("mapping cache TTL must be positive");
        }
        if (minimumCutoverDelay == null || minimumCutoverDelay.compareTo(mappingCacheTtl.multipliedBy(2)) < 0) {
            throw new IllegalStateException("minimum cutover delay must cover at least two mapping cache TTLs");
        }
        if (observationPeriod == null || observationPeriod.isZero() || observationPeriod.isNegative()) {
            throw new IllegalStateException("migration observation period must be positive");
        }
    }
}
