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
@TableName("virtual_shard_placement")
public class VirtualShardMigrationEntity {
    @TableId(value = "placement_id", type = IdType.INPUT)
    private String placementId;
    private String namespace;
    private Integer virtualShard;
    private Long mappingVersion;
    private Long epoch;
    private String migrationId;
    private String state;
    private String primaryPlacementJson;
    private String targetPlacementJson;
    private LocalDateTime cutoverNotBefore;
    private LocalDateTime observationUntil;
    private String copyCursor;
    private Long sourceRowCount;
    private Long targetRowCount;
    private String sourceChecksum;
    private String targetChecksum;
    private Long cdcLag;
    private Boolean cutoverCompleted;
    private String operatorName;
    private String failureReason;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
