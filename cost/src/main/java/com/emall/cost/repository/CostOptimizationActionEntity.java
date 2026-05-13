package com.emall.cost.repository;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("cost_optimization_action")
@Getter
@Setter
public class CostOptimizationActionEntity {
    @TableId(value = "action_id", type = IdType.INPUT)
    private Long actionId;

    @TableField("service_name")
    private String serviceName;

    @TableField("signal_type")
    private String signalType;

    @TableField("action_type")
    private String actionType;

    @TableField("status")
    private String status;

    @TableField("priority")
    private Integer priority;

    @TableField("description")
    private String description;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
