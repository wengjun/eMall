package com.emall.flashsale.repository;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("flash_sale_order_request")
@Getter
@Setter
public class FlashSaleOrderRequestEntity {
    @TableId(value = "request_id", type = IdType.INPUT)
    private Long requestId;

    @TableField("campaign_id")
    private Long campaignId;

    @TableField("user_id")
    private Long userId;

    @TableField("sku_id")
    private Long skuId;

    @TableField("quantity")
    private Integer quantity;

    @TableField("token")
    private String token;

    @TableField("status")
    private String status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
