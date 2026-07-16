package com.emall.identity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;

enum ProfileProjectionStatus {
    PENDING,
    READY,
    DELETION_PENDING,
    DELETED,
    CONFLICT
}

@TableName("identity_lifecycle")
record IdentityLifecycle(@TableId(value = "account_id", type = IdType.INPUT) long accountId, String registrationId,
        String requestFingerprint, String bindingHash, ProfileProjectionStatus projectionStatus,
        long lastPublishedVersion, long lastAcknowledgedVersion, int reconciliationPartition, Instant nextReconcileAt,
        Instant createdAt, Instant updatedAt) {
    IdentityLifecycle published(long version, ProfileProjectionStatus projection, Instant nextCheck) {
        return new IdentityLifecycle(accountId, registrationId, requestFingerprint, bindingHash, projection, version,
                lastAcknowledgedVersion, reconciliationPartition, nextCheck, createdAt, Instant.now());
    }

    IdentityLifecycle acknowledged(long identityVersion, ProfileProjectionStatus projection, Instant nextCheck) {
        return new IdentityLifecycle(accountId, registrationId, requestFingerprint, bindingHash, projection,
                lastPublishedVersion, Math.max(lastAcknowledgedVersion, identityVersion), reconciliationPartition,
                nextCheck, createdAt, Instant.now());
    }

    IdentityLifecycle eraseBinding(long identityVersion, Instant nextCheck) {
        return new IdentityLifecycle(accountId, registrationId, requestFingerprint, "", ProfileProjectionStatus.DELETED,
                lastPublishedVersion, Math.max(lastAcknowledgedVersion, identityVersion), reconciliationPartition,
                nextCheck, createdAt, Instant.now());
    }
}
