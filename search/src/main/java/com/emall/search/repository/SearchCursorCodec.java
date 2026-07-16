package com.emall.search.repository;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class SearchCursorCodec {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final Clock clock;

    @Autowired
    SearchCursorCodec(ObjectMapper objectMapper, @Value("${emall.search.elasticsearch.cursor-secret}") String secret) {
        this(objectMapper, secret, Clock.systemUTC());
    }

    SearchCursorCodec(ObjectMapper objectMapper, String secret, Clock clock) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("search cursor secret must contain at least 32 characters");
        }
        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.clock = clock;
    }

    String encode(SearchCursorState state) {
        try {
            byte[] payload = objectMapper.writeValueAsBytes(state);
            return encode(payload) + "." + encode(sign(payload));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to encode search cursor", ex);
        }
    }

    SearchCursorState decode(String token) {
        try {
            String[] parts = token == null ? new String[0] : token.split("\\.", -1);
            if (parts.length != 2) {
                throw invalidCursor();
            }
            byte[] payload = decodeBase64(parts[0]);
            byte[] signature = decodeBase64(parts[1]);
            if (!MessageDigest.isEqual(signature, sign(payload))) {
                throw invalidCursor();
            }
            SearchCursorState state = objectMapper.readValue(payload, SearchCursorState.class);
            if (state.expiresAt() == null || !state.expiresAt().isAfter(Instant.now(clock))) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "search cursor has expired");
            }
            return state;
        } catch (IllegalArgumentException | IOException ex) {
            throw invalidCursor();
        }
    }

    String fingerprint(String keyword, int pageSize) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String normalized = (keyword == null ? "" : keyword.strip().toLowerCase(Locale.ROOT)) + "\n" + pageSize;
            return encode(digest.digest(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private byte[] sign(byte[] payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return mac.doFinal(payload);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("failed to sign search cursor", ex);
        }
    }

    private String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private byte[] decodeBase64(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    private BusinessException invalidCursor() {
        return new BusinessException(ErrorCode.BAD_REQUEST, "invalid search cursor");
    }
}
