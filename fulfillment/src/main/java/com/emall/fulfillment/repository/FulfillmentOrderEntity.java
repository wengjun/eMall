package com.emall.fulfillment.repository;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("fulfillment_order")
public class FulfillmentOrderEntity {
    @TableId(value = "fulfillment_id", type = IdType.INPUT)
    private Long fulfillmentId;

    @TableField("order_id")
    private Long orderId;

    @TableField("user_id")
    private Long userId;

    @TableField("sku_id")
    private Long skuId;

    @TableField("quantity")
    private Integer quantity;

    @TableField("destination_region_code")
    private String destinationRegionCode;

    @TableField("warehouse_code")
    private String warehouseCode;

    @TableField("planned_carrier")
    private String plannedCarrier;

    @TableField("estimated_sla_hours")
    private Integer estimatedSlaHours;

    @TableField("carrier")
    private String carrier;

    @TableField("tracking_no")
    private String trackingNo;

    @TableField("status")
    private String status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    public Long getFulfillmentId() {
        return fulfillmentId;
    }

    public void setFulfillmentId(Long fulfillmentId) {
        this.fulfillmentId = fulfillmentId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getDestinationRegionCode() {
        return destinationRegionCode;
    }

    public void setDestinationRegionCode(String destinationRegionCode) {
        this.destinationRegionCode = destinationRegionCode;
    }

    public String getWarehouseCode() {
        return warehouseCode;
    }

    public void setWarehouseCode(String warehouseCode) {
        this.warehouseCode = warehouseCode;
    }

    public String getPlannedCarrier() {
        return plannedCarrier;
    }

    public void setPlannedCarrier(String plannedCarrier) {
        this.plannedCarrier = plannedCarrier;
    }

    public Integer getEstimatedSlaHours() {
        return estimatedSlaHours;
    }

    public void setEstimatedSlaHours(Integer estimatedSlaHours) {
        this.estimatedSlaHours = estimatedSlaHours;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public String getTrackingNo() {
        return trackingNo;
    }

    public void setTrackingNo(String trackingNo) {
        this.trackingNo = trackingNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
