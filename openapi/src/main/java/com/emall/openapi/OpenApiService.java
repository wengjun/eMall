package com.emall.openapi;

import com.emall.common.api.ErrorCode;
import com.emall.common.crypto.AesGcmFieldEncryptor;
import com.emall.common.crypto.FieldEncryptor;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class OpenApiService {
    private static final long SIGNATURE_WINDOW_SECONDS = 300L;

    private final OpenApiRepository repository;
    private final SnowflakeIdGenerator idGenerator;
    private final FieldEncryptor fieldEncryptor;

    OpenApiService(OpenApiRepository repository, SnowflakeIdGenerator idGenerator) {
        this(repository, idGenerator, new AesGcmFieldEncryptor("local-development-field-encryption-key"));
    }

    @Autowired
    OpenApiService(OpenApiRepository repository, SnowflakeIdGenerator idGenerator, FieldEncryptor fieldEncryptor) {
        this.repository = repository;
        this.idGenerator = idGenerator;
        this.fieldEncryptor = fieldEncryptor;
    }

    @Transactional
    AppRegistration registerApp(long merchantId, String name, String scopes, int dailyQuota) {
        if (dailyQuota < 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "openapi daily quota must be positive");
        }
        String appKey = "ak_" + UUID.randomUUID().toString().replace("-", "");
        String appSecret = "sk_" + UUID.randomUUID().toString().replace("-", "");
        Instant now = Instant.now();
        OpenApiApp app = repository
                .saveApp(new OpenApiApp(idGenerator.nextId(), merchantId, appKey, fieldEncryptor.lookupHash(appSecret),
                        fieldEncryptor.encrypt(appSecret), name, scopes, dailyQuota, true, now, now));
        return new AppRegistration(app, appSecret);
    }

    ApiSignatureVerification verifySignature(String appKey, String appSecret, String requestPath, String nonce,
            long timestamp, String signature) {
        OpenApiApp app = requireActiveApp(appKey);
        if (!secretMatches(appSecret, app.secretHash())) {
            return new ApiSignatureVerification(appKey, requestPath, nonce, timestamp, false, "invalid-secret");
        }
        long skew = Math.abs(Instant.now().getEpochSecond() - timestamp);
        if (skew > SIGNATURE_WINDOW_SECONDS) {
            return new ApiSignatureVerification(appKey, requestPath, nonce, timestamp, false, "timestamp-expired");
        }
        String payload = appKey + "\n" + requestPath + "\n" + nonce + "\n" + timestamp;
        boolean valid = hmacSha256(appSecret, payload).equals(signature);
        return new ApiSignatureVerification(appKey, requestPath, nonce, timestamp, valid,
                valid ? "valid" : "signature-mismatch");
    }

    @Transactional
    ApiRequestAuthentication authenticateRequest(String appKey, String requestPath, String nonce, long timestamp,
            String signature, String scope) {
        OpenApiApp app = requireActiveApp(appKey);
        String normalizedScope = normalize(scope);
        if (!scopeAllowed(app.scopes(), normalizedScope)) {
            return authentication(app, requestPath, normalizedScope, false, "scope-denied", null);
        }
        long skew = Math.abs(Instant.now().getEpochSecond() - timestamp);
        if (skew > SIGNATURE_WINDOW_SECONDS) {
            return authentication(app, requestPath, normalizedScope, false, "timestamp-expired", null);
        }
        String appSecret = decryptSigningSecret(app);
        String payload = appKey + "\n" + requestPath + "\n" + nonce + "\n" + timestamp;
        if (!hmacSha256(appSecret, payload).equals(signature)) {
            return authentication(app, requestPath, normalizedScope, false, "signature-mismatch", null);
        }
        boolean nonceAccepted = repository.saveNonceIfAbsent(appKey, normalize(nonce), requestPath,
                Instant.ofEpochSecond(timestamp + SIGNATURE_WINDOW_SECONDS));
        if (!nonceAccepted) {
            return authentication(app, requestPath, normalizedScope, false, "nonce-replayed", null);
        }
        ApiQuotaUsage quotaUsage = consumeQuota(appKey);
        if (!quotaUsage.allowed()) {
            return authentication(app, requestPath, normalizedScope, false, "quota-exceeded", quotaUsage);
        }
        return authentication(app, requestPath, normalizedScope, true, "allowed", quotaUsage);
    }

    @Transactional
    ApiQuotaUsage consumeQuota(String appKey) {
        OpenApiApp app = requireActiveApp(appKey);
        LocalDate today = LocalDate.now();
        ApiQuotaUsage current =
                repository.findQuota(appKey, today).orElse(new ApiQuotaUsage(appKey, today, 0, app.dailyQuota(), true));
        int nextCount = current.usedCount() + 1;
        boolean allowed = nextCount <= app.dailyQuota();
        return repository.saveQuota(new ApiQuotaUsage(appKey, today, nextCount, app.dailyQuota(), allowed));
    }

    @Transactional
    WebhookSubscription subscribe(long appId, String eventType, String targetUrl) {
        OpenApiApp app = repository.findApp(appId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "openapi app not found"));
        Instant now = Instant.now();
        return repository.saveSubscription(new WebhookSubscription(idGenerator.nextId(), app.appId(),
                normalize(eventType), targetUrl, true, now, now));
    }

    List<WebhookSubscription> findSubscriptions(long appId) {
        return repository.findActiveSubscriptions(appId);
    }

    @Transactional
    WebhookDelivery recordDelivery(long subscriptionId, String eventId, WebhookDeliveryStatus status) {
        repository.findSubscription(subscriptionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "webhook subscription not found"));
        Instant now = Instant.now();
        return repository.saveDelivery(
                new WebhookDelivery(idGenerator.nextId(), subscriptionId, normalize(eventId), status, 0, now, now));
    }

    String signatureFixture(String appSecret, String appKey, String requestPath, String nonce, long timestamp) {
        return hmacSha256(appSecret, appKey + "\n" + requestPath + "\n" + nonce + "\n" + timestamp);
    }

    private OpenApiApp requireActiveApp(String appKey) {
        OpenApiApp app = repository.findApp(appKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "openapi app not found"));
        if (!app.active()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "openapi app is not active");
        }
        return app;
    }

    private ApiRequestAuthentication authentication(OpenApiApp app, String requestPath, String scope, boolean allowed,
            String reason, ApiQuotaUsage quotaUsage) {
        return new ApiRequestAuthentication(app.appKey(), app.appId(), app.merchantId(), requestPath, scope, allowed,
                reason, quotaUsage);
    }

    private boolean scopeAllowed(String configuredScopes, String requiredScope) {
        String normalizedRequired = normalize(requiredScope);
        for (String scope : configuredScopes.split(",")) {
            String normalizedScope = scope.trim().toLowerCase(Locale.ROOT);
            if ("*".equals(normalizedScope) || normalizedScope.equals(normalizedRequired)) {
                return true;
            }
        }
        return false;
    }

    private String decryptSigningSecret(OpenApiApp app) {
        if (app.secretCiphertext() == null || app.secretCiphertext().isBlank()) {
            throw new BusinessException(ErrorCode.CONFLICT, "openapi signing secret is unavailable");
        }
        return fieldEncryptor.decrypt(app.secretCiphertext());
    }

    private boolean secretMatches(String appSecret, String storedHash) {
        return fieldEncryptor.lookupHash(appSecret).equals(storedHash) || sha256(appSecret).equals(storedHash);
    }

    private String normalize(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "openapi value must not be blank");
        }
        return normalized;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is unavailable", ex);
        }
    }

    private String hmacSha256(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("HMAC-SHA256 signing failed", ex);
        }
    }
}
