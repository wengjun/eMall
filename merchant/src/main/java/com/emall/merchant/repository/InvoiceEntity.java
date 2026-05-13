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

@TableName("merchant_invoice")
@Getter
@Setter
public class InvoiceEntity {
    @TableId(value = "invoice_id", type = IdType.INPUT)
    private Long invoiceId;

    @TableField("settlement_id")
    private Long settlementId;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("status")
    private String status;

    @TableField("invoice_title")
    private String invoiceTitle;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
