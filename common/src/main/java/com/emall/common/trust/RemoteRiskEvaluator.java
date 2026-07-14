package com.emall.common.trust;

import com.emall.common.api.ApiResponse;
import com.emall.common.security.AuthTokenCodec;
import java.util.Set;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

class RemoteRiskEvaluator implements RiskEvaluator {
    private static final ParameterizedTypeReference<ApiResponse<RiskDecision>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final RiskTrustProperties properties;
    private final AuthTokenCodec tokenCodec;
    private final String serviceName;

    RemoteRiskEvaluator(RestClient restClient, RiskTrustProperties properties, AuthTokenCodec tokenCodec,
            String serviceName) {
        this.restClient = restClient;
        this.properties = properties;
        this.tokenCodec = tokenCodec;
        this.serviceName = serviceName;
    }

    @Override
    public RiskDecision evaluate(RiskEvaluationRequest request) {
        try {
            ApiResponse<RiskDecision> response = restClient.post().uri("/api/risk/evaluate")
                    .header("Authorization", "Bearer " + serviceToken()).body(request).retrieve().body(RESPONSE_TYPE);
            if (response == null || !response.success() || response.data() == null) {
                return fallback("risk-invalid-response");
            }
            return response.data();
        } catch (RestClientException ex) {
            return fallback("risk-unavailable");
        }
    }

    private String serviceToken() {
        long serviceId = Integer.toUnsignedLong(serviceName.hashCode());
        return tokenCodec.issue(serviceId, serviceId, serviceName, "SERVICE_CLIENT", Set.of("risk:invoke"));
    }

    private RiskDecision fallback(String reason) {
        return properties.isFailClosed() ? new RiskDecision(RiskLevel.BLOCK, 100, reason) : RiskDecision.pass();
    }
}
