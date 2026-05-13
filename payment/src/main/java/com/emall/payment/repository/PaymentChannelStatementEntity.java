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

@TableName("payment_channel_statement")
@Getter
@Setter
public class PaymentChannelStatementEntity {
    @TableId(value = "statement_id", type = IdType.INPUT)
    private Long statementId;

    @TableField("channel")
    private String channel;

    @TableField("channel_trade_no")
    private String channelTradeNo;

    @TableField("payment_id")
    private Long paymentId;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("statement_type")
    private String statementType;

    @TableField("occurred_at")
    private LocalDateTime occurredAt;

    @TableField("reconciled")
    private Boolean reconciled;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
