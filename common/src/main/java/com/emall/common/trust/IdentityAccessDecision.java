package com.emall.common.trust;

public record IdentityAccessDecision(long accountId, String subject, String deviceId, boolean allowed, String reason) {
    public static IdentityAccessDecision allow(long accountId, String subject, String deviceId) {
        return new IdentityAccessDecision(accountId, subject, deviceId, true, "allowed");
    }

    public static IdentityAccessDecision deny(long accountId, String reason) {
        return new IdentityAccessDecision(accountId, null, null, false, reason);
    }
}
