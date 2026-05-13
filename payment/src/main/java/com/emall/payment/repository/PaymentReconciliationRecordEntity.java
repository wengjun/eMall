package com.emall.payment.repository;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("payment_reconciliation_record")
@Getter
@Setter
public class PaymentReconciliationRecordEntity {
    @TableId(value = "record_id", type = IdType.INPUT)
    private Long recordId;

    @TableField("statement_id")
    private Long statementId;

    @TableField("payment_id")
    private Long paymentId;

    @TableField("channel_trade_no")
    private String channelTradeNo;

    @TableField("statement_type")
    private String statementType;

    @TableField("status")
    private String status;

    @TableField("message")
    private String message;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
