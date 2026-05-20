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

@TableName("payment_refund_order")
@Getter
@Setter
public class PaymentRefundOrderEntity {
    @TableId(value = "refund_id", type = IdType.INPUT)
    private Long refundId;

    @TableField("payment_id")
    private Long paymentId;

    @TableField("request_id")
    private String requestId;

    @TableField("channel")
    private String channel;

    @TableField("channel_refund_no")
    private String channelRefundNo;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("status")
    private String status;

    @TableField("reason")
    private String reason;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
