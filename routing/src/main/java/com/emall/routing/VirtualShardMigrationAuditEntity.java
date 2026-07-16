package com.emall.routing;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("virtual_shard_migration_audit")
public class VirtualShardMigrationAuditEntity {
    @TableId(value = "audit_id", type = IdType.AUTO)
    private Long auditId;
    private String placementId;
    private String migrationId;
    private String fromState;
    private String toState;
    private Long previousVersion;
    private Long newVersion;
    private Long epoch;
    private String operatorName;
    private String snapshotJson;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
