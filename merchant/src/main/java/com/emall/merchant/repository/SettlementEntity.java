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

@TableName("merchant_settlement")
@Getter
@Setter
public class SettlementEntity {
    @TableId(value = "settlement_id", type = IdType.INPUT)
    private Long settlementId;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("gross_amount")
    private BigDecimal grossAmount;

    @TableField("commission_amount")
    private BigDecimal commissionAmount;

    @TableField("net_amount")
    private BigDecimal netAmount;

    @TableField("status")
    private String status;

    @TableField("period_start")
    private LocalDateTime periodStart;

    @TableField("period_end")
    private LocalDateTime periodEnd;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
