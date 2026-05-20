package com.emall.common.trust;

public record IdentityAccessRequest(String accessToken, long expectedAccountId, String scope, String resource,
        String deviceId, String sourceIp) {
}
