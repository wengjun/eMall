package com.emall.common.operations;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class InternalOperationAuthorizer {
    private InternalOperationAuthorizer() {
    }

    public static void requireAuthorized(String expectedToken, String actualToken) {
        if (expectedToken == null || expectedToken.isBlank()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "internal operations token is not configured");
        }
        if (actualToken == null || actualToken.isBlank()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "internal operations token is required");
        }
        byte[] expected = expectedToken.getBytes(StandardCharsets.UTF_8);
        byte[] actual = actualToken.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "internal operations token is invalid");
        }
    }
}
