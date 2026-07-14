package com.emall.payment.channel;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("emall.payment.channel")
public class PaymentChannelProperties {
    private String mode = "memory";
    private String baseUrl = "http://localhost:9090";
    private String apiKey = "";
    private Duration timeout = Duration.ofSeconds(3);
}
