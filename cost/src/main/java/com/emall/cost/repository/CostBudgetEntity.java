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

@TableName("cost_budget")
@Getter
@Setter
public class CostBudgetEntity {
    @TableId(value = "budget_id", type = IdType.INPUT)
    private Long budgetId;

    @TableField("service_name")
    private String serviceName;

    @TableField("monthly_budget")
    private BigDecimal monthlyBudget;

    @TableField("current_spend")
    private BigDecimal currentSpend;

    @TableField("currency")
    private String currency;

    @TableField("alert_threshold_percent")
    private Integer alertThresholdPercent;

    @TableField("active")
    private Boolean active;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
