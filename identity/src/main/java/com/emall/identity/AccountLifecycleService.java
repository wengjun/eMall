package com.emall.identity;

import com.emall.common.api.ErrorCode;
import com.emall.common.event.AccountLifecycleEventPayload;
import com.emall.common.event.EventTypes;
import com.emall.common.event.OutboxEvent;
import com.emall.common.event.UserProfileLifecycleEventPayload;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.common.outbox.OutboxRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AccountLifecycleService {
    private static final Pattern MOBILE = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Set<String> PROFILE_EVENTS = Set.of(EventTypes.USER_PROFILE_READY,
            EventTypes.USER_PROFILE_DELETION_COMPLETED, EventTypes.USER_PROFILE_RECONCILED);
    private static final int MINIMUM_PASSWORD_LENGTH = 12;
    private static final int MAXIMUM_PASSWORD_LENGTH = 128;

    private final IdentityRepository identityRepository;
    private final IdentityLifecycleRepository lifecycleRepository;
    private final OutboxRepository outboxRepository;
    private final IdentitySessionRevocationService sessionRevocationService;
    private final SnowflakeIdGenerator idGenerator;
    private final PasswordEncoder passwordEncoder;
    private final Duration pendingRetryInterval;
    private final Duration steadyStateReconciliationInterval;
    private final int reconciliationPartitions;

    AccountLifecycleService(IdentityRepository identityRepository, IdentityLifecycleRepository lifecycleRepository,
            OutboxRepository outboxRepository, IdentitySessionRevocationService sessionRevocationService,
            SnowflakeIdGenerator idGenerator, PasswordEncoder passwordEncoder,
            @Value("${emall.identity.lifecycle.pending-retry-interval:1m}") Duration pendingRetryInterval,
            @Value("${emall.identity.lifecycle.reconciliation-interval:24h}") Duration reconciliationInterval,
            @Value("${emall.identity.lifecycle.reconciliation-partitions:256}") int reconciliationPartitions) {
        this.identityRepository = identityRepository;
        this.lifecycleRepository = lifecycleRepository;
        this.outboxRepository = outboxRepository;
        this.sessionRevocationService = sessionRevocationService;
        this.idGenerator = idGenerator;
        this.passwordEncoder = passwordEncoder;
        this.pendingRetryInterval = pendingRetryInterval;
        this.steadyStateReconciliationInterval = reconciliationInterval;
        if (reconciliationPartitions <= 0 || reconciliationPartitions > 4_096) {
            throw new IllegalArgumentException("reconciliation partitions must be between 1 and 4096");
        }
        this.reconciliationPartitions = reconciliationPartitions;
    }

    @Transactional
    AccountRegistration register(String registrationId, String subject, String displayName, String password) {
        String normalizedRegistrationId = required(registrationId, "registration id", 128);
        String normalizedSubject = normalizeSubject(subject);
        String normalizedDisplayName = required(displayName, "display name", 128);
        validatePassword(password);
        String fingerprint = sha256(normalizedSubject + '\n' + normalizedDisplayName + '\n' + password);
        IdentityLifecycle replay = lifecycleRepository.findByRegistrationId(normalizedRegistrationId).orElse(null);
        if (replay != null) {
            if (!replay.requestFingerprint().equals(fingerprint)) {
                throw new BusinessException(ErrorCode.CONFLICT, "registration id was reused with another request");
            }
            return registration(replay);
        }
        identityRepository.findAccountBySubject(normalizedSubject).ifPresent(existing -> {
            throw new BusinessException(ErrorCode.CONFLICT, "identity subject already exists");
        });

        Instant now = Instant.now();
        long accountId = idGenerator.nextId();
        IdentityAccount account = identityRepository.saveAccount(new IdentityAccount(accountId, IdentityType.CUSTOMER,
                normalizedSubject, normalizedDisplayName, IdentityStatus.PENDING_PROFILE, now, now));
        identityRepository
                .saveCredential(new IdentityCredential(accountId, passwordEncoder.encode(password), 0, null, now, now));
        IdentityLifecycle lifecycle = new IdentityLifecycle(accountId, normalizedRegistrationId, fingerprint,
                bindingHash(normalizedSubject), ProfileProjectionStatus.PENDING, 0L, 0L,
                Math.floorMod(Long.hashCode(accountId), reconciliationPartitions), now.plus(pendingRetryInterval), now,
                now);
        lifecycleRepository.save(lifecycle);
        lifecycleRepository
                .save(publish(account, lifecycle, EventTypes.ACCOUNT_REGISTERED, "registration").publishedLifecycle());
        return registration(lifecycleRepository.findByAccountId(accountId).orElseThrow());
    }

    AccountRegistration registrationStatus(String registrationId) {
        IdentityLifecycle lifecycle =
                lifecycleRepository.findByRegistrationId(required(registrationId, "registration id", 128))
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "registration not found"));
        return registration(lifecycle);
    }

    @Transactional
    IdentityAccount changeStatus(long accountId, AccountLifecycleAction action, String reason) {
        IdentityAccount account = requireAccountForUpdate(accountId);
        String normalizedReason = required(reason, "lifecycle reason", 256);
        Transition transition = transition(account.status(), action);
        if (transition.idempotent()) {
            return account;
        }
        Instant now = Instant.now();
        if (!identityRepository.transitionAccountStatus(accountId, account.status(), transition.next(), now)) {
            IdentityAccount concurrent = requireAccount(accountId);
            if (concurrent.status() == transition.next()) {
                return concurrent;
            }
            throw new BusinessException(ErrorCode.CONFLICT, "identity lifecycle changed concurrently");
        }
        if (transition.next() != IdentityStatus.ACTIVE) {
            sessionRevocationService.revokeAll(accountId, now);
        }
        IdentityAccount changed = requireAccount(accountId);
        IdentityLifecycle lifecycle = requireLifecycleForUpdate(accountId);
        ProfileProjectionStatus projection = action == AccountLifecycleAction.DELETE
                ? ProfileProjectionStatus.DELETION_PENDING
                : ProfileProjectionStatus.PENDING;
        PublishedLifecycle published = publish(changed, lifecycle, transition.eventType(), normalizedReason);
        lifecycleRepository.save(published.publishedLifecycle().published(published.event().aggregateVersion(),
                projection, nextCheck(projection)));
        return changed;
    }

    @Transactional
    void handleProfileEvent(OutboxEvent event) {
        if (!PROFILE_EVENTS.contains(event.eventType())) {
            return;
        }
        UserProfileLifecycleEventPayload payload = UserProfileLifecycleEventPayload.from(event);
        IdentityAccount account = requireAccountForUpdate(payload.accountId());
        IdentityLifecycle lifecycle = requireLifecycleForUpdate(payload.accountId());
        if (!lifecycle.bindingHash().isBlank() && !lifecycle.bindingHash().equals(payload.bindingHash())) {
            lifecycleRepository.save(lifecycle.acknowledged(payload.identityEventVersion(),
                    ProfileProjectionStatus.CONFLICT, Instant.now().plus(pendingRetryInterval)));
            return;
        }
        if (EventTypes.USER_PROFILE_READY.equals(event.eventType())) {
            activateAfterProfileReady(payload, lifecycle, account);
        } else if (EventTypes.USER_PROFILE_DELETION_COMPLETED.equals(event.eventType())) {
            completeDeletion(payload, lifecycle, account);
        } else {
            acknowledgeReconciliation(payload, lifecycle, account);
        }
    }

    List<IdentityLifecycle> dueForReconciliation(int partition, Instant now, int limit) {
        return lifecycleRepository.findDueForReconciliation(partition, now, limit);
    }

    @Transactional
    void reconcile(long accountId) {
        IdentityAccount account = requireAccountForUpdate(accountId);
        IdentityLifecycle lifecycle = requireLifecycleForUpdate(accountId);
        if (account.status() == IdentityStatus.DELETED) {
            return;
        }
        PublishedLifecycle published =
                publish(account, lifecycle, EventTypes.ACCOUNT_RECONCILIATION_REQUESTED, "scheduled-reconciliation");
        lifecycleRepository.save(published.publishedLifecycle().published(published.event().aggregateVersion(),
                lifecycle.projectionStatus(), nextCheck(lifecycle.projectionStatus())));
    }

    private void activateAfterProfileReady(UserProfileLifecycleEventPayload payload, IdentityLifecycle lifecycle,
            IdentityAccount account) {
        if (account.status() == IdentityStatus.PENDING_PROFILE) {
            Instant now = Instant.now();
            if (!identityRepository.transitionAccountStatus(account.accountId(), IdentityStatus.PENDING_PROFILE,
                    IdentityStatus.ACTIVE, now)) {
                throw new BusinessException(ErrorCode.CONFLICT, "identity activation changed concurrently");
            }
            account = requireAccount(account.accountId());
            PublishedLifecycle published = publish(account, lifecycle, EventTypes.ACCOUNT_ACTIVATED, "profile-ready");
            lifecycle = published.publishedLifecycle().published(published.event().aggregateVersion(),
                    ProfileProjectionStatus.READY, nextCheck(ProfileProjectionStatus.READY));
        }
        lifecycleRepository.save(lifecycle.acknowledged(payload.identityEventVersion(), ProfileProjectionStatus.READY,
                nextCheck(ProfileProjectionStatus.READY)));
    }

    private void completeDeletion(UserProfileLifecycleEventPayload payload, IdentityLifecycle lifecycle,
            IdentityAccount account) {
        if (account.status() == IdentityStatus.DELETION_PENDING) {
            Instant now = Instant.now();
            if (!identityRepository.eraseAccount(account.accountId(), IdentityStatus.DELETION_PENDING, now)) {
                throw new BusinessException(ErrorCode.CONFLICT, "identity deletion changed concurrently");
            }
            identityRepository.deleteCredential(account.accountId());
            sessionRevocationService.revokeAll(account.accountId(), now);
            account = requireAccount(account.accountId());
            lifecycle = lifecycle.eraseBinding(payload.identityEventVersion(), Instant.now());
            PublishedLifecycle published = publish(account, lifecycle, EventTypes.ACCOUNT_DELETED, "profile-erased");
            lifecycle = published.publishedLifecycle().published(published.event().aggregateVersion(),
                    ProfileProjectionStatus.DELETED, Instant.now());
        }
        lifecycleRepository.save(lifecycle);
    }

    private void acknowledgeReconciliation(UserProfileLifecycleEventPayload payload, IdentityLifecycle lifecycle,
            IdentityAccount account) {
        String expected = expectedProfileStatus(account.status());
        ProfileProjectionStatus projection =
                expected.equals(payload.status()) ? projectionFor(account.status()) : ProfileProjectionStatus.CONFLICT;
        lifecycleRepository
                .save(lifecycle.acknowledged(payload.identityEventVersion(), projection, nextCheck(projection)));
    }

    private PublishedLifecycle publish(IdentityAccount account, IdentityLifecycle lifecycle, String eventType,
            String reason) {
        String subject = account.status() == IdentityStatus.DELETED ? "" : account.subject();
        String displayName = account.status() == IdentityStatus.DELETED ? "" : account.displayName();
        AccountLifecycleEventPayload payload = new AccountLifecycleEventPayload(account.accountId(), subject,
                displayName, lifecycle.bindingHash(), account.status().name(), reason);
        OutboxEvent event = outboxRepository.save(OutboxEvent.create("identity-event-" + idGenerator.nextId(),
                "IdentityAccount", Long.toString(account.accountId()), eventType, "identity", "1.0.0", payload));
        return new PublishedLifecycle(event, lifecycle.published(event.aggregateVersion(), lifecycle.projectionStatus(),
                lifecycle.nextReconcileAt()));
    }

    private AccountRegistration registration(IdentityLifecycle lifecycle) {
        IdentityAccount account = requireAccount(lifecycle.accountId());
        return new AccountRegistration(lifecycle.registrationId(), account.accountId(), account.status(),
                lifecycle.projectionStatus());
    }

    private Transition transition(IdentityStatus current, AccountLifecycleAction action) {
        return switch (action) {
            case SUSPEND ->
                transition(current, IdentityStatus.ACTIVE, IdentityStatus.SUSPENDED, EventTypes.ACCOUNT_SUSPENDED);
            case RESTORE ->
                transition(current, IdentityStatus.SUSPENDED, IdentityStatus.ACTIVE, EventTypes.ACCOUNT_RESTORED);
            case CLOSE -> transition(current, Set.of(IdentityStatus.ACTIVE, IdentityStatus.SUSPENDED),
                    IdentityStatus.CLOSED, EventTypes.ACCOUNT_CLOSED);
            case DELETE ->
                transition(current, Set.of(IdentityStatus.ACTIVE, IdentityStatus.SUSPENDED, IdentityStatus.CLOSED),
                        IdentityStatus.DELETION_PENDING, EventTypes.ACCOUNT_DELETION_REQUESTED);
        };
    }

    private Transition transition(IdentityStatus current, IdentityStatus expected, IdentityStatus next,
            String eventType) {
        return transition(current, Set.of(expected), next, eventType);
    }

    private Transition transition(IdentityStatus current, Set<IdentityStatus> expected, IdentityStatus next,
            String eventType) {
        if (current == next || current == IdentityStatus.DELETED
                || current == IdentityStatus.DELETION_PENDING && next == IdentityStatus.DELETION_PENDING) {
            return new Transition(next, eventType, true);
        }
        if (!expected.contains(current)) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "invalid identity lifecycle transition from " + current + " to " + next);
        }
        return new Transition(next, eventType, false);
    }

    private IdentityAccount requireAccount(long accountId) {
        return identityRepository.findAccount(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "identity account not found"));
    }

    private IdentityAccount requireAccountForUpdate(long accountId) {
        return identityRepository.findAccountForUpdate(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "identity account not found"));
    }

    private IdentityLifecycle requireLifecycle(long accountId) {
        return lifecycleRepository.findByAccountId(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "identity lifecycle not found"));
    }

    private IdentityLifecycle requireLifecycleForUpdate(long accountId) {
        return lifecycleRepository.findByAccountIdForUpdate(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "identity lifecycle not found"));
    }

    private Instant nextCheck(ProfileProjectionStatus status) {
        Duration interval = status == ProfileProjectionStatus.PENDING || status == ProfileProjectionStatus.CONFLICT
                || status == ProfileProjectionStatus.DELETION_PENDING
                        ? pendingRetryInterval
                        : steadyStateReconciliationInterval;
        return Instant.now().plus(interval);
    }

    private ProfileProjectionStatus projectionFor(IdentityStatus status) {
        return status == IdentityStatus.DELETED ? ProfileProjectionStatus.DELETED : ProfileProjectionStatus.READY;
    }

    private String expectedProfileStatus(IdentityStatus status) {
        return switch (status) {
            case PENDING_PROFILE, ACTIVE -> "NORMAL";
            case SUSPENDED, LOCKED -> "FROZEN";
            case CLOSED, DELETION_PENDING, DELETED -> "CLOSED";
        };
    }

    private String normalizeSubject(String value) {
        String normalized = required(value, "mobile", 32).toLowerCase(Locale.ROOT);
        if (!MOBILE.matcher(normalized).matches()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "customer identity must be a valid mobile number");
        }
        return normalized;
    }

    private String required(String value, String field, int maximumLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank() || normalized.length() > maximumLength) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    field + " must contain 1 to " + maximumLength + " characters");
        }
        return normalized;
    }

    private void validatePassword(String password) {
        boolean validLength = password != null && password.length() >= MINIMUM_PASSWORD_LENGTH
                && password.length() <= MAXIMUM_PASSWORD_LENGTH;
        boolean validComplexity = validLength && password.chars().anyMatch(Character::isUpperCase)
                && password.chars().anyMatch(Character::isLowerCase) && password.chars().anyMatch(Character::isDigit);
        if (!validComplexity) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "credential must be 12 to 128 characters and include "
                    + "upper-case, lower-case, and numeric characters");
        }
    }

    private String bindingHash(String subject) {
        return sha256("identity-binding-v1:" + subject);
    }

    private String sha256(String value) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is unavailable", exception);
        }
    }

    private record Transition(IdentityStatus next, String eventType, boolean idempotent) {
    }

    private record PublishedLifecycle(OutboxEvent event, IdentityLifecycle publishedLifecycle) {
    }
}
