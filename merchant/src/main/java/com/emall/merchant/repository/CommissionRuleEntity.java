package com.emall.merchant.repository;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("merchant_commission_rule")
@Getter
@Setter
public class CommissionRuleEntity {
    @TableId(value = "rule_id", type = IdType.INPUT)
    private Long ruleId;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("rate")
    private BigDecimal rate;

    @TableField("active")
    private Boolean active;

    @TableField("effective_from")
    private LocalDateTime effectiveFrom;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
