package com.emall.payment.repository;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("payment_ledger_entry")
@Getter
@Setter
public class PaymentLedgerEntryEntity {
    @TableId(value = "ledger_id", type = IdType.INPUT)
    private Long ledgerId;

    @TableField("payment_id")
    private Long paymentId;

    @TableField("order_id")
    private Long orderId;

    @TableField("user_id")
    private Long userId;

    @TableField("direction")
    private String direction;

    @TableField("account_code")
    private String accountCode;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("currency")
    private String currency;

    @TableField("business_type")
    private String businessType;

    @TableField("reference_id")
    private String referenceId;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
