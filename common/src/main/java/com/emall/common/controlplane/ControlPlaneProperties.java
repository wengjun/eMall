package com.emall.common.controlplane;

import java.time.Duration;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("emall.control-plane")
public class ControlPlaneProperties {
    private boolean enabled;
    private String instanceId;
    private int batchSize = 20;
    private int maxAttempts = 5;
    private int rollbackAttempts = 3;
    private Duration leaseDuration = Duration.ofSeconds(30);
    private Duration retryBaseDelay = Duration.ofSeconds(1);
    private Duration reconcileDelay = Duration.ofSeconds(1);
    private List<ControlPlaneTarget> requiredTargets = List.of();
    private Nacos nacos = new Nacos();
    private Kubernetes kubernetes = new Kubernetes();
    private Kafka kafka = new Kafka();
    private Infrastructure infrastructure = new Infrastructure();

    @Data
    public static class Nacos {
        private boolean enabled;
        private String baseUrl = "http://localhost:8848";
        private String namespace = "public";
        private String group = "EMALL_CONTROL_PLANE";
        private String accessToken;
    }

    @Data
    public static class Kubernetes {
        private boolean enabled;
        private String baseUrl = "https://kubernetes.default.svc";
        private String namespace = "emall";
        private String bearerToken;
        private String bearerTokenFile = "/var/run/secrets/kubernetes.io/serviceaccount/token";
        private String caCertificateFile = "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt";
    }

    @Data
    public static class Kafka {
        private boolean enabled;
        private Duration timeout = Duration.ofSeconds(10);
    }

    @Data
    public static class Infrastructure {
        private boolean enabled;
        private String baseUrl = "http://localhost:8120";
        private String bearerToken;
    }
}
