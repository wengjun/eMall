package com.emall.common.trust;

import com.emall.common.api.ApiResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

class RemoteIdentityVerifier implements IdentityVerifier {
    private static final ParameterizedTypeReference<ApiResponse<IdentityValidationPayload>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final IdentityTrustProperties properties;

    RemoteIdentityVerifier(RestClient restClient, IdentityTrustProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public IdentityAccessDecision verify(IdentityAccessRequest request) {
        try {
            ApiResponse<IdentityValidationPayload> response = restClient.post().uri("/api/identity/sessions/validate")
                    .body(new ValidateSessionRequest(request.accessToken(), request.scope(), request.resource()))
                    .retrieve().body(RESPONSE_TYPE);
            if (response == null || !response.success() || response.data() == null) {
                return denyOrSkip(request, "identity-invalid-response");
            }
            IdentityValidationPayload payload = response.data();
            return new IdentityAccessDecision(payload.accountId(), payload.subject(), payload.deviceId(),
                    payload.allowed(), payload.reason());
        } catch (RestClientException ex) {
            return denyOrSkip(request, "identity-unavailable");
        }
    }

    private IdentityAccessDecision denyOrSkip(IdentityAccessRequest request, String reason) {
        if (properties.isFailClosed()) {
            return IdentityAccessDecision.deny(request.expectedAccountId(), reason);
        }
        return IdentityAccessDecision.allow(request.expectedAccountId(), String.valueOf(request.expectedAccountId()),
                request.deviceId());
    }

    private record ValidateSessionRequest(String accessToken, String scope, String resource) {
    }

    private record IdentityValidationPayload(long accountId, String subject, String deviceId, boolean allowed,
            String reason) {
    }
}
