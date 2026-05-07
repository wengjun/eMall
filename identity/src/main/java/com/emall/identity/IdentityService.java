package com.emall.identity;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class IdentityService {
    private final IdentityRepository repository;
    private final SnowflakeIdGenerator idGenerator;

    IdentityService(IdentityRepository repository, SnowflakeIdGenerator idGenerator) {
        this.repository = repository;
        this.idGenerator = idGenerator;
    }

    @Transactional
    IdentityAccount createAccount(IdentityType type, String subject, String displayName) {
        String normalizedSubject = normalize(subject);
        repository.findAccountBySubject(normalizedSubject).ifPresent(existing -> {
            throw new BusinessException(ErrorCode.CONFLICT, "identity subject already exists");
        });
        Instant now = Instant.now();
        return repository.saveAccount(new IdentityAccount(idGenerator.nextId(), type, normalizedSubject, displayName,
                IdentityStatus.ACTIVE, now, now));
    }

    @Transactional
    AuthToken login(String subject, String deviceId) {
        IdentityAccount account = repository.findAccountBySubject(normalize(subject))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "identity account not found"));
        if (account.status() != IdentityStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "identity account is not active");
        }
        Instant now = Instant.now();
        Instant expiresAt = now.plus(2, ChronoUnit.HOURS);
        DeviceSession session = repository.saveSession(new DeviceSession(idGenerator.nextId(), account.accountId(),
                normalize(deviceId), tokenValue(), tokenValue(), SessionStatus.ACTIVE, expiresAt, now, now));
        return new AuthToken(session.sessionId(), session.accessToken(), session.refreshToken(), session.expiresAt());
    }

    @Transactional
    DeviceSession revokeSession(long sessionId) {
        DeviceSession session = repository.findSession(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "identity session not found"));
        return repository.saveSession(session.revoke());
    }

    @Transactional
    PermissionGrant grantPermission(long accountId, String scope, String resource) {
        requireAccount(accountId);
        return repository.saveGrant(new PermissionGrant(idGenerator.nextId(), accountId, normalize(scope),
                normalizeResource(resource), Instant.now()));
    }

    AccessDecision checkAccess(long accountId, String scope, String resource) {
        requireAccount(accountId);
        String normalizedScope = normalize(scope);
        String normalizedResource = normalizeResource(resource);
        boolean allowed =
                repository.findGrants(accountId).stream().anyMatch(grant -> grant.scope().equals(normalizedScope)
                        && ("*".equals(grant.resource()) || grant.resource().equals(normalizedResource)));
        return new AccessDecision(accountId, normalizedScope, normalizedResource, allowed);
    }

    @Transactional
    ServiceClient registerServiceClient(String clientKey, String clientSecret, String scopes) {
        Instant now = Instant.now();
        return repository.saveServiceClient(new ServiceClient(idGenerator.nextId(), normalize(clientKey),
                sha256(clientSecret), normalizeResource(scopes), true, now, now));
    }

    @Transactional
    MerchantSubAccount createMerchantSubAccount(long merchantId, long accountId, String roleCode) {
        requireAccount(accountId);
        Instant now = Instant.now();
        return repository.saveSubAccount(new MerchantSubAccount(idGenerator.nextId(), merchantId, accountId,
                normalize(roleCode), true, now, now));
    }

    private IdentityAccount requireAccount(long accountId) {
        return repository.findAccount(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "identity account not found"));
    }

    private String tokenValue() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String normalize(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "identity value must not be blank");
        }
        return normalized;
    }

    private String normalizeResource(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "identity resource must not be blank");
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
}
