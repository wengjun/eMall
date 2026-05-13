package com.emall.flashsale.repository;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("flash_sale_stock")
@Getter
@Setter
public class FlashSaleStockEntity {
    @TableId(value = "campaign_id", type = IdType.INPUT)
    private Long campaignId;

    @TableField("sku_id")
    private Long skuId;

    @TableField("total_stock")
    private Integer totalStock;

    @TableField("available_stock")
    private Integer availableStock;

    @TableField("token_reserved_stock")
    private Integer tokenReservedStock;

    @TableField("queued_stock")
    private Integer queuedStock;

    @TableField("sold_stock")
    private Integer soldStock;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
