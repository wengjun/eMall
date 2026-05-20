package com.emall.cost.repository;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("service_capacity_baseline")
@Getter
@Setter
public class CostCapacityBaselineEntity {
    @TableId(value = "baseline_id", type = IdType.INPUT)
    private Long baselineId;

    @TableField("service_name")
    private String serviceName;

    @TableField("safe_qps")
    private Integer safeQps;

    @TableField("peak_qps")
    private Integer peakQps;

    @TableField("current_qps")
    private Integer currentQps;

    @TableField("current_replicas")
    private Integer currentReplicas;

    @TableField("max_replicas")
    private Integer maxReplicas;

    @TableField("cpu_utilization")
    private BigDecimal cpuUtilization;

    @TableField("memory_utilization")
    private BigDecimal memoryUtilization;

    @TableField("monthly_cost")
    private BigDecimal monthlyCost;

    @TableField("slo_protected")
    private Boolean sloProtected;

    @TableField("risk_level")
    private String riskLevel;

    @TableField("recommendation")
    private String recommendation;

    @TableField("observed_at")
    private LocalDateTime observedAt;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
