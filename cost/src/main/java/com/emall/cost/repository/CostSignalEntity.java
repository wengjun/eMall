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

@TableName("cost_signal")
@Getter
@Setter
public class CostSignalEntity {
    @TableId(value = "signal_id", type = IdType.INPUT)
    private Long signalId;

    @TableField("service_name")
    private String serviceName;

    @TableField("signal_type")
    private String signalType;

    @TableField("metric_value")
    private BigDecimal metricValue;

    @TableField("threshold_value")
    private BigDecimal thresholdValue;

    @TableField("observed_at")
    private LocalDateTime observedAt;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
