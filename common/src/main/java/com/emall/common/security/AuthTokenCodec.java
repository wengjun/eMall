package com.emall.common.security;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class AuthTokenCodec {
    private static final String VERSION = "v1";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final ObjectMapper objectMapper;
    private final AuthSecurityProperties properties;
    private final Clock clock;

    public AuthTokenCodec(ObjectMapper objectMapper, AuthSecurityProperties properties) {
        this(objectMapper, properties, Clock.systemUTC());
    }

    AuthTokenCodec(ObjectMapper objectMapper, AuthSecurityProperties properties, Clock clock) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.clock = clock;
    }

    public String issue(long accountId, long sessionId, String subject, String identityType, Set<String> scopes) {
        Instant issuedAt = clock.instant();
        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(accountId, sessionId, subject, identityType,
                scopes, issuedAt, issuedAt.plus(properties.getAccessTokenTtl()));
        return issue(principal);
    }

    public String issue(AuthenticatedPrincipal principal) {
        try {
            TokenPayload payload = new TokenPayload(properties.getIssuer(), principal.accountId(),
                    principal.sessionId(), principal.subject(), principal.identityType(), principal.scopes(),
                    principal.issuedAt().getEpochSecond(), principal.expiresAt().getEpochSecond());
            String encodedPayload =
                    Base64.getUrlEncoder().withoutPadding().encodeToString(objectMapper.writeValueAsBytes(payload));
            String signingInput = VERSION + "." + encodedPayload;
            return signingInput + "." + Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(sign(signingInput, properties.getTokenSecret()));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize access token", ex);
        }
    }

    public AuthenticatedPrincipal verify(String token) {
        try {
            String[] parts = token == null ? new String[0] : token.split("\\.", -1);
            if (parts.length != 3 || !VERSION.equals(parts[0])) {
                throw unauthorized("access token format is invalid");
            }
            String signingInput = parts[0] + "." + parts[1];
            byte[] suppliedSignature = Base64.getUrlDecoder().decode(parts[2]);
            if (!hasValidSignature(signingInput, suppliedSignature)) {
                throw unauthorized("access token signature is invalid");
            }
            TokenPayload payload = objectMapper.readValue(Base64.getUrlDecoder().decode(parts[1]), TokenPayload.class);
            if (!properties.getIssuer().equals(payload.issuer())) {
                throw unauthorized("access token issuer is invalid");
            }
            Instant issuedAt = Instant.ofEpochSecond(payload.issuedAt());
            Instant expiresAt = Instant.ofEpochSecond(payload.expiresAt());
            Instant now = clock.instant();
            if (!expiresAt.isAfter(now) || issuedAt.isAfter(now.plusSeconds(30))) {
                throw unauthorized("access token has expired or is not active");
            }
            if (payload.accountId() <= 0 || payload.sessionId() <= 0 || payload.subject() == null
                    || payload.identityType() == null) {
                throw unauthorized("access token claims are invalid");
            }
            return new AuthenticatedPrincipal(payload.accountId(), payload.sessionId(), payload.subject(),
                    payload.identityType(), payload.scopes(), issuedAt, expiresAt);
        } catch (BusinessException ex) {
            throw ex;
        } catch (IllegalArgumentException | IOException ex) {
            throw unauthorized("access token is invalid");
        }
    }

    private boolean hasValidSignature(String input, byte[] suppliedSignature) {
        List<String> secrets = new ArrayList<>();
        secrets.add(properties.getTokenSecret());
        secrets.addAll(properties.getPreviousTokenSecrets());
        boolean valid = false;
        for (String secret : secrets) {
            valid |= java.security.MessageDigest.isEqual(sign(input, secret), suppliedSignature);
        }
        return valid;
    }

    private byte[] sign(String input, String configuredSecret) {
        byte[] secret = configuredSecret == null ? new byte[0] : configuredSecret.getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("Authentication token secret must contain at least 32 UTF-8 bytes");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable", ex);
        }
    }

    private BusinessException unauthorized(String message) {
        return new BusinessException(ErrorCode.UNAUTHORIZED, message);
    }

    private record TokenPayload(String issuer, long accountId, long sessionId, String subject, String identityType,
            Set<String> scopes, long issuedAt, long expiresAt) {
    }
}
