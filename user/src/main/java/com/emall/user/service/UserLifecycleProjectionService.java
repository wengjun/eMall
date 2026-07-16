package com.emall.user.service;

import com.emall.common.event.AccountLifecycleEventPayload;
import com.emall.common.event.EventTypes;
import com.emall.common.event.OutboxEvent;
import com.emall.common.event.UserProfileLifecycleEventPayload;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.common.outbox.OutboxRepository;
import com.emall.user.domain.UserAccount;
import com.emall.user.domain.UserStatus;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserLifecycleProjectionService {
    public static final Set<String> SUPPORTED_EVENTS =
            Set.of(EventTypes.ACCOUNT_REGISTERED, EventTypes.ACCOUNT_ACTIVATED, EventTypes.ACCOUNT_SUSPENDED,
                    EventTypes.ACCOUNT_RESTORED, EventTypes.ACCOUNT_CLOSED, EventTypes.ACCOUNT_DELETION_REQUESTED,
                    EventTypes.ACCOUNT_DELETED, EventTypes.ACCOUNT_RECONCILIATION_REQUESTED);

    private final UserService userService;
    private final OutboxRepository outboxRepository;
    private final SnowflakeIdGenerator idGenerator;

    public UserLifecycleProjectionService(UserService userService, OutboxRepository outboxRepository,
            SnowflakeIdGenerator idGenerator) {
        this.userService = userService;
        this.outboxRepository = outboxRepository;
        this.idGenerator = idGenerator;
    }

    @Transactional
    public void apply(OutboxEvent event) {
        if (!SUPPORTED_EVENTS.contains(event.eventType())) {
            return;
        }
        AccountLifecycleEventPayload payload = AccountLifecycleEventPayload.from(event);
        ProjectionResult result = switch (event.eventType()) {
            case EventTypes.ACCOUNT_REGISTERED -> ready(provision(payload));
            case EventTypes.ACCOUNT_ACTIVATED, EventTypes.ACCOUNT_RESTORED ->
                reconciled(project(payload, UserStatus.NORMAL));
            case EventTypes.ACCOUNT_SUSPENDED -> reconciled(project(payload, UserStatus.FROZEN));
            case EventTypes.ACCOUNT_CLOSED -> reconciled(project(payload, UserStatus.CLOSED));
            case EventTypes.ACCOUNT_DELETION_REQUESTED -> deletionCompleted(erase(payload));
            case EventTypes.ACCOUNT_DELETED -> reconciled(erase(payload));
            case EventTypes.ACCOUNT_RECONCILIATION_REQUESTED -> reconcile(payload);
            default -> throw new IllegalStateException("unsupported identity lifecycle event: " + event.eventType());
        };
        publish(result, payload, event.aggregateVersion());
    }

    private ProjectionResult reconcile(AccountLifecycleEventPayload payload) {
        return switch (payload.status()) {
            case "PENDING_PROFILE" -> ready(provision(payload));
            case "ACTIVE" -> reconciled(project(payload, UserStatus.NORMAL));
            case "LOCKED", "SUSPENDED" -> reconciled(project(payload, UserStatus.FROZEN));
            case "CLOSED" -> reconciled(project(payload, UserStatus.CLOSED));
            case "DELETION_PENDING" -> deletionCompleted(erase(payload));
            case "DELETED" -> reconciled(erase(payload));
            default -> throw new IllegalStateException("unsupported identity status: " + payload.status());
        };
    }

    private UserStatus provision(AccountLifecycleEventPayload payload) {
        return userService.provisionFromIdentity(payload.accountId(), payload.subject(), payload.displayName(),
                payload.bindingHash()).status();
    }

    private UserStatus project(AccountLifecycleEventPayload payload, UserStatus status) {
        provision(payload);
        return userService.projectStatusFromIdentity(payload.accountId(), payload.bindingHash(), status).status();
    }

    private UserStatus erase(AccountLifecycleEventPayload payload) {
        UserAccount erased = userService.eraseFromIdentity(payload.accountId(), payload.bindingHash());
        return erased == null ? UserStatus.CLOSED : erased.status();
    }

    private ProjectionResult ready(UserStatus status) {
        return new ProjectionResult(EventTypes.USER_PROFILE_READY, status);
    }

    private ProjectionResult deletionCompleted(UserStatus status) {
        return new ProjectionResult(EventTypes.USER_PROFILE_DELETION_COMPLETED, status);
    }

    private ProjectionResult reconciled(UserStatus status) {
        return new ProjectionResult(EventTypes.USER_PROFILE_RECONCILED, status);
    }

    private void publish(ProjectionResult result, AccountLifecycleEventPayload source, long identityEventVersion) {
        UserProfileLifecycleEventPayload payload = new UserProfileLifecycleEventPayload(source.accountId(),
                source.bindingHash(), result.status().name(), identityEventVersion);
        outboxRepository.save(OutboxEvent.create("user-profile-event-" + idGenerator.nextId(), "UserProfile",
                Long.toString(source.accountId()), result.eventType(), "user", "1.0.0", payload));
    }

    private record ProjectionResult(String eventType, UserStatus status) {
    }
}
