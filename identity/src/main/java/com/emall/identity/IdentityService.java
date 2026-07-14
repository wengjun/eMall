package com.emall.identity;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.common.security.AuthSecurityProperties;
import com.emall.common.security.AuthTokenCodec;
import com.emall.common.security.AuthorizationGuard;
import com.emall.common.security.TokenRevocationStore;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class IdentityService {
    private static final int MINIMUM_PASSWORD_LENGTH = 12;
    private static final int MAXIMUM_PASSWORD_LENGTH = 128;
    private final IdentityRepository repository;
    private final SnowflakeIdGenerator idGenerator;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenCodec tokenCodec;
    private final AuthSecurityProperties authProperties;
    private final TokenRevocationStore revocationStore;
    private final CredentialAttemptRecorder attemptRecorder;
    private final AuthorizationGuard authorizationGuard;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String dummyPasswordHash;

    IdentityService(IdentityRepository repository, SnowflakeIdGenerator idGenerator, PasswordEncoder passwordEncoder,
            AuthTokenCodec tokenCodec, AuthSecurityProperties authProperties, TokenRevocationStore revocationStore,
            CredentialAttemptRecorder attemptRecorder, AuthorizationGuard authorizationGuard) {
        this.repository = repository;
        this.idGenerator = idGenerator;
        this.passwordEncoder = passwordEncoder;
        this.tokenCodec = tokenCodec;
        this.authProperties = authProperties;
        this.revocationStore = revocationStore;
        this.attemptRecorder = attemptRecorder;
        this.authorizationGuard = authorizationGuard;
        this.dummyPasswordHash = passwordEncoder.encode(randomToken());
    }

    @Transactional
    IdentityAccount createAccount(IdentityType type, String subject, String displayName, String password) {
        validatePassword(password);
        String normalizedSubject = normalize(subject);
        repository.findAccountBySubject(normalizedSubject).ifPresent(existing -> {
            throw new BusinessException(ErrorCode.CONFLICT, "identity subject already exists");
        });
        Instant now = Instant.now();
        IdentityAccount account = repository.saveAccount(new IdentityAccount(idGenerator.nextId(), type,
                normalizedSubject, normalizeDisplayName(displayName), IdentityStatus.ACTIVE, now, now));
        repository.saveCredential(
                new IdentityCredential(account.accountId(), passwordEncoder.encode(password), 0, null, now, now));
        return account;
    }

    AuthToken login(String subject, String password, String deviceId) {
        String normalizedSubject = normalize(subject);
        IdentityAccount account = repository.findAccountBySubject(normalizedSubject).orElse(null);
        if (account == null) {
            passwordEncoder.matches(password == null ? "" : password, dummyPasswordHash);
            throw invalidCredentials();
        }
        IdentityCredential credential =
                repository.findCredential(account.accountId()).orElseThrow(this::invalidCredentials);
        Instant now = Instant.now();
        if (credential.lockedUntil() != null && credential.lockedUntil().isAfter(now)) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "identity credential is temporarily locked");
        }
        if (!passwordEncoder.matches(password == null ? "" : password, credential.passwordHash())) {
            attemptRecorder.recordFailure(credential, now);
            throw invalidCredentials();
        }
        if (account.status() != IdentityStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "identity account is not active");
        }
        attemptRecorder.recordSuccess(credential, now);
        return issueSession(account, normalize(deviceId), now);
    }

    @Transactional
    AuthToken refresh(String refreshToken, String deviceId) {
        String tokenHash = sha256(normalizeToken(refreshToken));
        DeviceSession existing = repository.findSessionByRefreshToken(tokenHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "refresh token is invalid"));
        Instant now = Instant.now();
        if (existing.status() != SessionStatus.ACTIVE || !existing.expiresAt().isAfter(now)
                || !existing.deviceId().equals(normalize(deviceId))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "refresh token is expired, revoked, or device-bound");
        }
        IdentityAccount account = requireAccount(existing.accountId());
        if (!repository.revokeSessionIfActive(existing.sessionId(), tokenHash, now)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "refresh token has already been used");
        }
        revokeAccessToken(existing, now);
        return issueSession(account, existing.deviceId(), now);
    }

    @Transactional
    DeviceSession revokeSession(long sessionId) {
        DeviceSession session = repository.findSession(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "identity session not found"));
        authorizationGuard.requireOwnerOrOperator(session.accountId());
        Instant now = Instant.now();
        repository.revokeSessionIfActive(session.sessionId(), session.refreshToken(), now);
        revokeAccessToken(session, now);
        return repository.findSession(sessionId).orElseGet(session::revoke);
    }

    @Transactional
    PermissionGrant grantPermission(long accountId, String scope, String resource) {
        authorizationGuard.requireOperator();
        requireAccount(accountId);
        return repository.saveGrant(new PermissionGrant(idGenerator.nextId(), accountId, normalize(scope),
                normalizeResource(resource), Instant.now()));
    }

    AccessDecision checkAccess(long accountId, String scope, String resource) {
        authorizationGuard.requireOwnerOrOperator(accountId);
        requireAccount(accountId);
        String normalizedScope = normalize(scope);
        String normalizedResource = normalizeResource(resource);
        boolean allowed = hasGrant(accountId, normalizedScope, normalizedResource);
        return new AccessDecision(accountId, normalizedScope, normalizedResource, allowed);
    }

    SessionValidation validateSession(String accessToken, String scope, String resource) {
        var principal = tokenCodec.verify(normalizeToken(accessToken));
        authorizationGuard.requireAccount(principal.accountId());
        DeviceSession session = repository.findSessionByAccessToken(sha256(accessToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "identity session not found"));
        if (principal.sessionId() != session.sessionId() || principal.accountId() != session.accountId()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "identity session claims do not match");
        }
        IdentityAccount account = requireAccount(session.accountId());
        if (account.status() != IdentityStatus.ACTIVE) {
            return new SessionValidation(account.accountId(), account.subject(), session.deviceId(), false,
                    "account-not-active");
        }
        if (session.status() != SessionStatus.ACTIVE) {
            return new SessionValidation(account.accountId(), account.subject(), session.deviceId(), false,
                    "session-not-active");
        }
        if (!session.expiresAt().isAfter(Instant.now())) {
            return new SessionValidation(account.accountId(), account.subject(), session.deviceId(), false,
                    "session-expired");
        }
        boolean allowed = hasGrant(account.accountId(), normalize(scope), normalizeResource(resource));
        return new SessionValidation(account.accountId(), account.subject(), session.deviceId(), allowed,
                allowed ? "allowed" : "permission-denied");
    }

    @Transactional
    ServiceClient registerServiceClient(String clientKey, String clientSecret, String scopes) {
        authorizationGuard.requireOperator();
        validatePassword(clientSecret);
        Instant now = Instant.now();
        return repository.saveServiceClient(new ServiceClient(idGenerator.nextId(), normalize(clientKey),
                passwordEncoder.encode(clientSecret), normalizeScopes(scopes), true, now, now));
    }

    AuthToken authenticateServiceClient(String clientKey, String clientSecret) {
        ServiceClient client = repository.findServiceClient(normalize(clientKey)).orElse(null);
        if (client == null || !client.active()
                || !passwordEncoder.matches(clientSecret == null ? "" : clientSecret, client.secretHash())) {
            passwordEncoder.matches(clientSecret == null ? "" : clientSecret, dummyPasswordHash);
            throw invalidCredentials();
        }
        Instant now = Instant.now();
        String accessToken = tokenCodec.issue(client.clientId(), client.clientId(), client.clientKey(),
                IdentityType.SERVICE_CLIENT.name(), parseScopes(client.scopes()));
        return new AuthToken(client.clientId(), accessToken, null, now.plus(authProperties.getAccessTokenTtl()), null);
    }

    @Transactional
    MerchantSubAccount createMerchantSubAccount(long merchantId, long accountId, String roleCode) {
        authorizationGuard.requireOperator();
        requireAccount(accountId);
        Instant now = Instant.now();
        return repository.saveSubAccount(new MerchantSubAccount(idGenerator.nextId(), merchantId, accountId,
                normalize(roleCode), true, now, now));
    }

    private AuthToken issueSession(IdentityAccount account, String deviceId, Instant now) {
        long sessionId = idGenerator.nextId();
        Set<String> scopes = repository.findGrants(account.accountId()).stream().map(PermissionGrant::scope)
                .collect(Collectors.toSet());
        String accessToken =
                tokenCodec.issue(account.accountId(), sessionId, account.subject(), account.type().name(), scopes);
        String refreshToken = randomToken();
        Instant accessExpiresAt = now.plus(authProperties.getAccessTokenTtl());
        Instant refreshExpiresAt = now.plus(30, ChronoUnit.DAYS);
        repository.saveSession(new DeviceSession(sessionId, account.accountId(), deviceId, sha256(accessToken),
                sha256(refreshToken), SessionStatus.ACTIVE, refreshExpiresAt, now, now));
        return new AuthToken(sessionId, accessToken, refreshToken, accessExpiresAt, refreshExpiresAt);
    }

    private void revokeAccessToken(DeviceSession session, Instant now) {
        java.time.Duration remaining = java.time.Duration.between(now, session.expiresAt());
        if (!remaining.isNegative() && !remaining.isZero()) {
            revocationStore.revoke(session.sessionId(), remaining);
        }
    }

    private IdentityAccount requireAccount(long accountId) {
        return repository.findAccount(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "identity account not found"));
    }

    private boolean hasGrant(long accountId, String scope, String resource) {
        return repository.findGrants(accountId).stream()
                .anyMatch(grant -> (grant.scope().equals(scope) || "*".equals(grant.scope()))
                        && ("*".equals(grant.resource()) || grant.resource().equals(resource)));
    }

    private String randomToken() {
        byte[] value = new byte[32];
        secureRandom.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private String normalize(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "identity value must not be blank");
        }
        return normalized;
    }

    private String normalizeDisplayName(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank() || normalized.length() > 128) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "display name must contain 1 to 128 characters");
        }
        return normalized;
    }

    private String normalizeToken(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "identity token must not be blank");
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

    private String normalizeScopes(String value) {
        Set<String> scopes = parseScopes(value);
        if (scopes.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "service client scopes must not be blank");
        }
        return String.join(",", scopes);
    }

    private Set<String> parseScopes(String value) {
        if (value == null) {
            return Set.of();
        }
        return Arrays.stream(value.split(",")).map(String::trim).filter(scope -> !scope.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    private void validatePassword(String password) {
        boolean lengthValid = password != null && password.length() >= MINIMUM_PASSWORD_LENGTH
                && password.length() <= MAXIMUM_PASSWORD_LENGTH;
        boolean complexityValid = lengthValid && password.chars().anyMatch(Character::isUpperCase)
                && password.chars().anyMatch(Character::isLowerCase) && password.chars().anyMatch(Character::isDigit);
        if (!complexityValid) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "credential must be 12 to 128 characters and include "
                    + "upper-case, lower-case, and numeric characters");
        }
    }

    private BusinessException invalidCredentials() {
        return new BusinessException(ErrorCode.UNAUTHORIZED, "identity credentials are invalid");
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
