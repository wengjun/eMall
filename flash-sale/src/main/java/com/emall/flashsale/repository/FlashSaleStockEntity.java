package com.emall.flashsale.repository;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("flash_sale_stock")
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

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(Long campaignId) {
        this.campaignId = campaignId;
    }

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public Integer getTotalStock() {
        return totalStock;
    }

    public void setTotalStock(Integer totalStock) {
        this.totalStock = totalStock;
    }

    public Integer getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(Integer availableStock) {
        this.availableStock = availableStock;
    }

    public Integer getTokenReservedStock() {
        return tokenReservedStock;
    }

    public void setTokenReservedStock(Integer tokenReservedStock) {
        this.tokenReservedStock = tokenReservedStock;
    }

    public Integer getQueuedStock() {
        return queuedStock;
    }

    public void setQueuedStock(Integer queuedStock) {
        this.queuedStock = queuedStock;
    }

    public Integer getSoldStock() {
        return soldStock;
    }

    public void setSoldStock(Integer soldStock) {
        this.soldStock = soldStock;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
