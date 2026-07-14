package com.emall.common.id;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("emall.id")
public class SnowflakeIdProperties {
    private Long workerId;
    private boolean leaseEnabled;
    private Duration leaseTtl = Duration.ofSeconds(60);
    private Duration renewInterval = Duration.ofSeconds(15);
    private long maximumClockRollbackMillis = 5;
}
