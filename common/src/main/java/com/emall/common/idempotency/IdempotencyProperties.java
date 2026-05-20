package com.emall.common.idempotency;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Data;

@ConfigurationProperties("emall.idempotency")
@Data
public class IdempotencyProperties {
    private Duration processingTtl = Duration.ofSeconds(30);
    private Duration recordTtl = Duration.ofDays(1);
}
