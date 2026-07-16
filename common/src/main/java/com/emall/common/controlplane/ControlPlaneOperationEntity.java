package com.emall.common.controlplane;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Data;

@Data
@TableName("control_plane_operation")
public class ControlPlaneOperationEntity {
    @TableId(value = "operation_id", type = IdType.INPUT)
    private String operationId;
    private String idempotencyKey;
    private String moduleName;
    private String targetType;
    private String actionName;
    private String resourceType;
    private String resourceId;
    private String desiredState;
    private String desiredDigest;
    private String rollbackState;
    private String observedState;
    private String status;
    private int attemptCount;
    private int maxAttempts;
    private Instant nextAttemptAt;
    private String leaseOwner;
    private Instant leaseUntil;
    private String lastError;
    private Instant createdAt;
    private Instant updatedAt;
}
