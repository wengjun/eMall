package com.emall.order.repository;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("order_record")
@Getter
@Setter
public class OrderEntity {
    @TableId(value = "order_id", type = IdType.INPUT)
    private Long orderId;

    @TableField("request_id")
    private String requestId;

    @TableField("user_id")
    private Long userId;

    @TableField("sku_id")
    private Long skuId;

    @TableField("quantity")
    private Integer quantity;

    @TableField("client_type")
    private String clientType;

    @TableField("unit_price")
    private BigDecimal unitPrice;

    @TableField("subtotal_amount")
    private BigDecimal subtotalAmount;

    @TableField("discount_amount")
    private BigDecimal discountAmount;

    @TableField("payable_amount")
    private BigDecimal payableAmount;

    @TableField("currency")
    private String currency;

    @TableField("price_version")
    private Long priceVersion;

    @TableField("coupon_id")
    private String couponId;

    @TableField("inventory_reservation_id")
    private String inventoryReservationId;

    @TableField("status")
    private String status;

    @TableField("failure_reason")
    private String failureReason;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
