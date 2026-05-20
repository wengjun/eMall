package com.emall.common.trust;

import com.emall.common.api.ApiResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

class RemoteRiskEvaluator implements RiskEvaluator {
    private static final ParameterizedTypeReference<ApiResponse<RiskDecision>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final RiskTrustProperties properties;

    RemoteRiskEvaluator(RestClient restClient, RiskTrustProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public RiskDecision evaluate(RiskEvaluationRequest request) {
        try {
            ApiResponse<RiskDecision> response =
                    restClient.post().uri("/api/risk/evaluate").body(request).retrieve().body(RESPONSE_TYPE);
            if (response == null || !response.success() || response.data() == null) {
                return fallback("risk-invalid-response");
            }
            return response.data();
        } catch (RestClientException ex) {
            return fallback("risk-unavailable");
        }
    }

    private RiskDecision fallback(String reason) {
        return properties.isFailClosed() ? new RiskDecision(RiskLevel.BLOCK, 100, reason) : RiskDecision.pass();
    }
}
