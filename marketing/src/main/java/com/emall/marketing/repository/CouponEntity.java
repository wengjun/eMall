package com.emall.marketing.repository;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("coupon")
@Getter
@Setter
public class CouponEntity {
    @TableId(value = "coupon_id", type = IdType.INPUT)
    private String couponId;

    @TableField("user_id")
    private Long userId;

    @TableField("threshold_amount")
    private BigDecimal thresholdAmount;

    @TableField("discount_amount")
    private BigDecimal discountAmount;

    @TableField("status")
    private String status;

    @TableField("expires_at")
    private LocalDateTime expiresAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
