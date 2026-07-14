package com.emall.common.security;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("emall.security.auth")
public class AuthSecurityProperties {
    private boolean enabled = true;
    private String issuer = "emall-identity";
    private String tokenSecret = "local-dev-auth-token-secret-change-before-production";
    private List<String> previousTokenSecrets = new ArrayList<>();
    private Duration accessTokenTtl = Duration.ofMinutes(10);
    private boolean failClosedOnRevocationStoreError;
    private List<String> publicEndpoints = new ArrayList<>(List.of("GET:/actuator/health/**", "GET:/actuator/info",
            "GET:/api/products/**", "GET:/api/catalog/**", "GET:/api/search/**", "POST:/api/identity/accounts",
            "POST:/api/identity/sessions", "POST:/api/identity/sessions/refresh", "POST:/api/identity/service-sessions",
            "POST:/api/payments/*/callbacks"));
    private List<String> customerEndpoints = new ArrayList<>(List.of("*:/api/users/**", "*:/api/carts/**",
            "*:/api/orders/**", "*:/api/payments/**", "*:/api/after-sales/**", "PATCH:/api/identity/sessions/*/revoke",
            "POST:/api/identity/sessions/validate", "GET:/api/identity/access"));

    public void setPreviousTokenSecrets(List<String> previousTokenSecrets) {
        this.previousTokenSecrets = new ArrayList<>(previousTokenSecrets);
    }

    public void setPublicEndpoints(List<String> publicEndpoints) {
        this.publicEndpoints = new ArrayList<>(publicEndpoints);
    }

    public void setCustomerEndpoints(List<String> customerEndpoints) {
        this.customerEndpoints = new ArrayList<>(customerEndpoints);
    }
}
