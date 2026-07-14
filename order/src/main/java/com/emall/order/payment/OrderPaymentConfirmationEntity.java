package com.emall.order.payment;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("order_payment_confirmation")
@Getter
@Setter
class OrderPaymentConfirmationEntity {
    @TableId(value = "order_id", type = IdType.INPUT)
    private Long orderId;
    private Long paymentId;
    private BigDecimal paidAmount;
    private String currency;
    private String channelTradeNo;
    private LocalDateTime confirmedAt;
}
