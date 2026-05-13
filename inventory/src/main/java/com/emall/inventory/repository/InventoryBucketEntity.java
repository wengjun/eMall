package com.emall.inventory.repository;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("inventory_bucket")
public class InventoryBucketEntity {
    @TableId(value = "sku_id", type = IdType.INPUT)
    private Long skuId;

    @TableField("bucket_no")
    private Integer bucketNo;

    @TableField("total")
    private Integer total;

    @TableField("reserved")
    private Integer reserved;

    @TableField("sold")
    private Integer sold;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public Integer getBucketNo() {
        return bucketNo;
    }

    public void setBucketNo(Integer bucketNo) {
        this.bucketNo = bucketNo;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public Integer getReserved() {
        return reserved;
    }

    public void setReserved(Integer reserved) {
        this.reserved = reserved;
    }

    public Integer getSold() {
        return sold;
    }

    public void setSold(Integer sold) {
        this.sold = sold;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
