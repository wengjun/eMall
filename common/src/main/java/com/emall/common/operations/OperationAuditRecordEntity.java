package com.emall.common.operations;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("internal_operation_audit")
@Getter
@Setter
public class OperationAuditRecordEntity {
    @TableId(value = "audit_id", type = IdType.AUTO)
    private Long auditId;

    @TableField("service_name")
    private String serviceName;

    @TableField("operation")
    private String operation;

    @TableField("operator")
    private String operator;

    @TableField("trace_id")
    private String traceId;

    @TableField("affected")
    private Integer affected;

    @TableField("success")
    private Boolean success;

    @TableField("message")
    private String message;

    @TableField("role")
    private String role;

    @TableField("approval_id")
    private String approvalId;

    @TableField("source_identity")
    private String sourceIdentity;

    @TableField("parameter_digest")
    private String parameterDigest;

    @TableField("executed_at")
    private LocalDateTime executedAt;
}
