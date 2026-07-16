package com.emall.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.event.AccountLifecycleEventPayload;
import com.emall.common.event.EventTypes;
import com.emall.common.event.OutboxEvent;
import com.emall.common.event.UserProfileLifecycleEventPayload;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.common.security.AuthSecurityProperties;
import com.emall.common.security.AuthTokenCodec;
import com.emall.common.security.AuthorizationGuard;
import com.emall.common.security.TokenRevocationStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AccountLifecycleServiceTest {
    private static final String MOBILE = "13800000000";
    private static final String PASSWORD = "StrongPassword123";

    private final InMemoryIdentityRepository identityRepository = new InMemoryIdentityRepository();
    private final InMemoryIdentityLifecycleRepository lifecycleRepository = new InMemoryIdentityLifecycleRepository();
    private final InMemoryIdentityOutboxRepository outboxRepository = new InMemoryIdentityOutboxRepository();
    private final RecordingRevocationStore revocationStore = new RecordingRevocationStore();
    private final AuthSecurityProperties securityProperties = new AuthSecurityProperties();
    private AccountLifecycleService lifecycleService;
    private IdentityService identityService;

    @BeforeEach
    void setUp() {
        securityProperties.setTokenSecret("identity-lifecycle-unit-test-secret-32-bytes");
        SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(22L);
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        IdentitySessionRevocationService sessionRevocationService =
                new IdentitySessionRevocationService(identityRepository, revocationStore, securityProperties);
        lifecycleService = new AccountLifecycleService(identityRepository, lifecycleRepository, outboxRepository,
                sessionRevocationService, idGenerator, passwordEncoder, Duration.ofMinutes(1), Duration.ofHours(24), 4);
        identityService = new IdentityService(identityRepository, idGenerator, passwordEncoder,
                new AuthTokenCodec(new ObjectMapper(), securityProperties), securityProperties, revocationStore,
                new CredentialAttemptRecorder(identityRepository), AuthorizationGuard.noop());
    }

    @Test
    void registersOnceAndActivatesOnlyAfterProfileAcknowledgement() {
        AccountRegistration first = lifecycleService.register("registration-1", MOBILE, "Alice", PASSWORD);
        AccountRegistration replay = lifecycleService.register("registration-1", MOBILE, "Alice", PASSWORD);

        assertThat(first.accountStatus()).isEqualTo(IdentityStatus.PENDING_PROFILE);
        assertThat(replay.accountId()).isEqualTo(first.accountId());
        assertThatThrownBy(() -> identityService.login(MOBILE, PASSWORD, "device-1"))
                .hasMessageContaining("not active");
        assertThatThrownBy(() -> lifecycleService.register("registration-1", MOBILE, "Other", PASSWORD))
                .hasMessageContaining("reused");

        OutboxEvent registered = nextOutboxEvent();
        AccountLifecycleEventPayload registeredPayload = AccountLifecycleEventPayload.from(registered);
        acknowledge(first.accountId(), registeredPayload.bindingHash(), EventTypes.USER_PROFILE_READY, "NORMAL",
                registered.aggregateVersion(), 1L);

        AccountRegistration active = lifecycleService.registrationStatus("registration-1");
        assertThat(active.accountStatus()).isEqualTo(IdentityStatus.ACTIVE);
        assertThat(active.profileStatus()).isEqualTo(ProfileProjectionStatus.READY);
        assertThat(identityService.login(MOBILE, PASSWORD, "device-1").accessToken()).isNotBlank();
    }

    @Test
    void suspendsRestoresAndRevokesEveryExistingSession() {
        AccountRegistration registration = registerAndActivate("registration-2", "13900000000");
        AuthToken oldToken = identityService.login("13900000000", PASSWORD, "device-2");

        IdentityAccount suspended =
                lifecycleService.changeStatus(registration.accountId(), AccountLifecycleAction.SUSPEND, "risk-control");

        assertThat(suspended.status()).isEqualTo(IdentityStatus.SUSPENDED);
        assertThat(revocationStore.revokedSessions).contains(oldToken.sessionId());
        assertThat(identityRepository.findSession(oldToken.sessionId())).get().extracting(DeviceSession::status)
                .isEqualTo(SessionStatus.REVOKED);
        assertThatThrownBy(() -> identityService.login("13900000000", PASSWORD, "device-3"))
                .hasMessageContaining("not active");

        IdentityAccount restored =
                lifecycleService.changeStatus(registration.accountId(), AccountLifecycleAction.RESTORE, "risk-cleared");

        assertThat(restored.status()).isEqualTo(IdentityStatus.ACTIVE);
        assertThatThrownBy(() -> identityService.refresh(oldToken.refreshToken(), "device-2"))
                .hasMessageContaining("expired, revoked");
        assertThat(identityService.login("13900000000", PASSWORD, "device-3").accessToken()).isNotBlank();
    }

    @Test
    void erasesIdentityOnlyAfterProfileDeletionAcknowledgement() {
        AccountRegistration registration = registerAndActivate("registration-3", "13700000000");
        lifecycleService.changeStatus(registration.accountId(), AccountLifecycleAction.DELETE, "privacy-request");
        IdentityLifecycle deletionPending = lifecycleRepository.findByAccountId(registration.accountId()).orElseThrow();

        acknowledge(registration.accountId(), deletionPending.bindingHash(), EventTypes.USER_PROFILE_DELETION_COMPLETED,
                "CLOSED", deletionPending.lastPublishedVersion(), 2L);

        IdentityAccount deleted = identityRepository.findAccount(registration.accountId()).orElseThrow();
        assertThat(deleted.status()).isEqualTo(IdentityStatus.DELETED);
        assertThat(deleted.subject()).startsWith("deleted-");
        assertThat(identityRepository.findCredential(registration.accountId())).isEmpty();
        assertThat(lifecycleRepository.findByAccountId(registration.accountId())).get()
                .extracting(IdentityLifecycle::projectionStatus).isEqualTo(ProfileProjectionStatus.DELETED);
    }

    @Test
    void detectsProfileBindingConflictWithoutActivatingAccount() {
        AccountRegistration registration = lifecycleService.register("registration-4", "13600000000", "Bob", PASSWORD);
        OutboxEvent registered = nextOutboxEvent();

        acknowledge(registration.accountId(), "wrong-binding", EventTypes.USER_PROFILE_READY, "NORMAL",
                registered.aggregateVersion(), 1L);

        assertThat(lifecycleService.registrationStatus("registration-4").accountStatus())
                .isEqualTo(IdentityStatus.PENDING_PROFILE);
        assertThat(lifecycleRepository.findByAccountId(registration.accountId())).get()
                .extracting(IdentityLifecycle::projectionStatus).isEqualTo(ProfileProjectionStatus.CONFLICT);
    }

    private AccountRegistration registerAndActivate(String registrationId, String mobile) {
        AccountRegistration registration =
                lifecycleService.register(registrationId, mobile, "Lifecycle User", PASSWORD);
        OutboxEvent registered = nextOutboxEvent();
        AccountLifecycleEventPayload payload = AccountLifecycleEventPayload.from(registered);
        acknowledge(registration.accountId(), payload.bindingHash(), EventTypes.USER_PROFILE_READY, "NORMAL",
                registered.aggregateVersion(), 1L);
        outboxRepository.save(registered.published());
        return lifecycleService.registrationStatus(registrationId);
    }

    private OutboxEvent nextOutboxEvent() {
        return outboxRepository.findPublishable(Instant.now().plusSeconds(1), 1).get(0);
    }

    private void acknowledge(long accountId, String bindingHash, String eventType, String status, long identityVersion,
            long profileVersion) {
        OutboxEvent acknowledgement = OutboxEvent
                .create("profile-event-" + accountId + '-' + profileVersion, "UserProfile", Long.toString(accountId),
                        eventType, "user", "1.0.0",
                        new UserProfileLifecycleEventPayload(accountId, bindingHash, status, identityVersion))
                .withAggregateVersion(profileVersion);
        lifecycleService.handleProfileEvent(acknowledgement);
    }

    private static final class RecordingRevocationStore implements TokenRevocationStore {
        private final Set<Long> revokedSessions = ConcurrentHashMap.newKeySet();

        @Override
        public boolean isRevoked(long sessionId) {
            return revokedSessions.contains(sessionId);
        }

        @Override
        public void revoke(long sessionId, Duration ttl) {
            revokedSessions.add(sessionId);
        }
    }
}
