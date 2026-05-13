package com.emall.common.operations;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("internal_operation_audit")
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

    @TableField("executed_at")
    private LocalDateTime executedAt;

    public Long getAuditId() {
        return auditId;
    }

    public void setAuditId(Long auditId) {
        this.auditId = auditId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Integer getAffected() {
        return affected;
    }

    public void setAffected(Integer affected) {
        this.affected = affected;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }
}
