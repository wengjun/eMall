package com.emall.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AuthTokenCodecTest {
    private static final String OLD_SECRET = "old-authentication-signing-secret-32-bytes";
    private static final String NEW_SECRET = "new-authentication-signing-secret-32-bytes";

    @Test
    void acceptsPreviousKeyDuringRotationAndIssuesOnlyWithCurrentKey() {
        AuthSecurityProperties oldProperties = properties(OLD_SECRET);
        String oldToken = new AuthTokenCodec(new ObjectMapper(), oldProperties).issue(1001L, 2001L, "customer",
                "CUSTOMER", Set.of("order:read"));
        AuthSecurityProperties rotatedProperties = properties(NEW_SECRET);
        rotatedProperties.setPreviousTokenSecrets(List.of(OLD_SECRET));
        AuthTokenCodec rotatedCodec = new AuthTokenCodec(new ObjectMapper(), rotatedProperties);

        assertThat(rotatedCodec.verify(oldToken).accountId()).isEqualTo(1001L);
        String newToken = rotatedCodec.issue(1001L, 2002L, "customer", "CUSTOMER", Set.of());
        assertThatThrownBy(() -> new AuthTokenCodec(new ObjectMapper(), oldProperties).verify(newToken))
                .isInstanceOf(BusinessException.class).hasMessageContaining("signature");
    }

    private AuthSecurityProperties properties(String secret) {
        AuthSecurityProperties properties = new AuthSecurityProperties();
        properties.setTokenSecret(secret);
        return properties;
    }
}
