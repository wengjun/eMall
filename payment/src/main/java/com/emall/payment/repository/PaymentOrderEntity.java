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

@TableName("payment_order")
@Getter
@Setter
public class PaymentOrderEntity {
    @TableId(value = "payment_id", type = IdType.INPUT)
    private Long paymentId;

    @TableField("request_id")
    private String requestId;

    @TableField("order_id")
    private Long orderId;

    @TableField("user_id")
    private Long userId;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("channel")
    private String channel;

    @TableField("channel_trade_no")
    private String channelTradeNo;

    @TableField("status")
    private String status;

    @TableField("order_confirmed")
    private Boolean orderConfirmed;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
