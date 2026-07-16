package com.emall.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.event.AccountLifecycleEventPayload;
import com.emall.common.event.EventTypes;
import com.emall.common.event.OutboxEvent;
import com.emall.common.event.UserProfileLifecycleEventPayload;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.user.domain.UserStatus;
import com.emall.user.repository.InMemoryUserOutboxRepository;
import com.emall.user.repository.InMemoryUserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class UserLifecycleProjectionServiceTest {
    private static final long ACCOUNT_ID = 9001L;
    private static final String MOBILE = "13800000001";

    private final InMemoryUserRepository userRepository = new InMemoryUserRepository();
    private final InMemoryUserOutboxRepository outboxRepository = new InMemoryUserOutboxRepository();
    private final UserLifecycleProjectionService projectionService =
            new UserLifecycleProjectionService(new UserService(userRepository, new SnowflakeIdGenerator(30L)),
                    outboxRepository, new SnowflakeIdGenerator(31L));

    @Test
    void projectsRegistrationSuspensionRestorationAndDeletionWithAcknowledgements() {
        projectionService.apply(
                identityEvent(1L, EventTypes.ACCOUNT_REGISTERED, "PENDING_PROFILE", MOBILE, bindingHash(MOBILE)));
        assertThat(userRepository.findById(ACCOUNT_ID)).get().extracting(user -> user.status())
                .isEqualTo(UserStatus.NORMAL);
        assertAcknowledgement(EventTypes.USER_PROFILE_READY, 1L);

        projectionService
                .apply(identityEvent(2L, EventTypes.ACCOUNT_SUSPENDED, "SUSPENDED", MOBILE, bindingHash(MOBILE)));
        assertThat(userRepository.findById(ACCOUNT_ID)).get().extracting(user -> user.status())
                .isEqualTo(UserStatus.FROZEN);
        assertAcknowledgement(EventTypes.USER_PROFILE_RECONCILED, 2L);

        projectionService.apply(identityEvent(3L, EventTypes.ACCOUNT_RESTORED, "ACTIVE", MOBILE, bindingHash(MOBILE)));
        assertThat(userRepository.findById(ACCOUNT_ID)).get().extracting(user -> user.status())
                .isEqualTo(UserStatus.NORMAL);
        assertAcknowledgement(EventTypes.USER_PROFILE_RECONCILED, 3L);

        projectionService.apply(identityEvent(4L, EventTypes.ACCOUNT_DELETION_REQUESTED, "DELETION_PENDING", MOBILE,
                bindingHash(MOBILE)));
        assertThat(userRepository.findById(ACCOUNT_ID)).get().satisfies(user -> {
            assertThat(user.status()).isEqualTo(UserStatus.CLOSED);
            assertThat(user.mobile()).startsWith("deleted-");
            assertThat(user.nickname()).startsWith("deleted-user-");
        });
        assertAcknowledgement(EventTypes.USER_PROFILE_DELETION_COMPLETED, 4L);
    }

    @Test
    void repairsMissingProfileDuringReconciliation() {
        projectionService.apply(
                identityEvent(7L, EventTypes.ACCOUNT_RECONCILIATION_REQUESTED, "ACTIVE", MOBILE, bindingHash(MOBILE)));

        assertThat(userRepository.findById(ACCOUNT_ID)).isPresent();
        assertAcknowledgement(EventTypes.USER_PROFILE_RECONCILED, 7L);
    }

    @Test
    void rejectsIdentityAndProfileBindingMismatch() {
        assertThatThrownBy(() -> projectionService
                .apply(identityEvent(1L, EventTypes.ACCOUNT_REGISTERED, "PENDING_PROFILE", MOBILE, "wrong-binding")))
                .hasMessageContaining("binding hash");

        assertThat(userRepository.findById(ACCOUNT_ID)).isEmpty();
        assertThat(outboxRepository.findPublishable(Instant.now().plusSeconds(1), 10)).isEmpty();
    }

    private void assertAcknowledgement(String eventType, long identityVersion) {
        OutboxEvent event = outboxRepository.findPublishable(Instant.now().plusSeconds(1), 1).get(0);
        UserProfileLifecycleEventPayload payload = UserProfileLifecycleEventPayload.from(event);
        assertThat(event.eventType()).isEqualTo(eventType);
        assertThat(payload.identityEventVersion()).isEqualTo(identityVersion);
        outboxRepository.save(event.published());
    }

    private OutboxEvent identityEvent(long version, String eventType, String status, String subject,
            String bindingHash) {
        return OutboxEvent.create("identity-event-" + version, "IdentityAccount", Long.toString(ACCOUNT_ID), eventType,
                "identity", "1.0.0",
                new AccountLifecycleEventPayload(ACCOUNT_ID, subject, "Lifecycle User", bindingHash, status, "test"))
                .withAggregateVersion(version);
    }

    private String bindingHash(String mobile) {
        try {
            byte[] value = ("identity-binding-v1:" + mobile).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
