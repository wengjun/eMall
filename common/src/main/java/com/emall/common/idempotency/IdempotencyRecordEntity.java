package com.emall.common.idempotency;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("idempotency_record")
@Getter
@Setter
public class IdempotencyRecordEntity {
    @TableId(value = "idempotency_key", type = IdType.INPUT)
    private String key;

    @TableField("request_id")
    private String requestId;

    @TableField("business_type")
    private String businessType;

    @TableField("business_id")
    private String businessId;

    @TableField("operation")
    private String operation;

    @TableField("owner_id")
    private String ownerId;

    @TableField("request_digest")
    private String requestDigest;

    @TableField("response_digest")
    private String responseDigest;

    @TableField("status")
    private String status;

    @TableField("locked_until")
    private LocalDateTime lockedUntil;

    @TableField("expires_at")
    private LocalDateTime expiresAt;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
