package com.emall.user.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.common.event.AccountLifecycleEventPayload;
import com.emall.common.event.EventTypes;
import com.emall.common.event.OutboxEvent;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.common.messaging.InMemoryProcessedMessageRepository;
import com.emall.common.messaging.InMemoryAggregateVersionGuard;
import com.emall.common.metrics.BusinessMetrics;
import com.emall.common.sharding.ShardRoutingOperations;
import com.emall.user.repository.InMemoryUserOutboxRepository;
import com.emall.user.repository.InMemoryUserRepository;
import com.emall.user.service.UserLifecycleProjectionService;
import com.emall.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class IdentityAccountEventConsumerTest {
    @Test
    void consumesDuplicateRegistrationExactlyOnce() throws Exception {
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        InMemoryUserOutboxRepository outboxRepository = new InMemoryUserOutboxRepository();
        UserLifecycleProjectionService projectionService =
                new UserLifecycleProjectionService(new UserService(userRepository, new SnowflakeIdGenerator(32L)),
                        outboxRepository, new SnowflakeIdGenerator(33L));
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        AtomicLong routedAccountId = new AtomicLong();
        ShardRoutingOperations routing = new ShardRoutingOperations() {
            @Override
            public <T> T execute(String logicalTable, long shardKey, Supplier<T> action) {
                routedAccountId.set(shardKey);
                return action.get();
            }

            @Override
            public <T> T execute(String logicalTable, String shardKey, Supplier<T> action) {
                return action.get();
            }
        };
        IdentityAccountEventConsumer consumer = new IdentityAccountEventConsumer(objectMapper, projectionService,
                BusinessMetrics.noop(), new InMemoryProcessedMessageRepository(), 3, null,
                new InMemoryAggregateVersionGuard(), routing);
        long accountId = 9101L;
        String mobile = "13800000002";
        String bindingHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(("identity-binding-v1:" + mobile).getBytes(StandardCharsets.UTF_8)));
        OutboxEvent event =
                OutboxEvent
                        .create("identity-register-9101", "IdentityAccount", Long.toString(accountId),
                                EventTypes.ACCOUNT_REGISTERED, "identity", "1.0.0", new AccountLifecycleEventPayload(
                                        accountId, mobile, "Alice", bindingHash, "PENDING_PROFILE", "registration"))
                        .withAggregateVersion(1L);
        String message = objectMapper.writeValueAsString(event);

        consumer.onIdentityEvent(message);
        consumer.onIdentityEvent(message);

        assertThat(userRepository.findById(accountId)).isPresent();
        assertThat(routedAccountId).hasValue(accountId);
        assertThat(outboxRepository.findPublishable(Instant.now().plusSeconds(1), 10)).hasSize(1).first()
                .extracting(OutboxEvent::eventType).isEqualTo(EventTypes.USER_PROFILE_READY);
    }
}
