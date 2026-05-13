package com.emall.fulfillment.repository;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("fulfillment_carrier_route")
public class FulfillmentCarrierRouteEntity {
    @TableId(value = "route_id", type = IdType.INPUT)
    private Long routeId;

    @TableField("carrier_code")
    private String carrierCode;

    @TableField("origin_warehouse_code")
    private String originWarehouseCode;

    @TableField("destination_region_code")
    private String destinationRegionCode;

    @TableField("priority")
    private Integer priority;

    @TableField("base_cost")
    private BigDecimal baseCost;

    @TableField("sla_hours")
    private Integer slaHours;

    @TableField("active")
    private Boolean active;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getRouteId() {
        return routeId;
    }

    public void setRouteId(Long routeId) {
        this.routeId = routeId;
    }

    public String getCarrierCode() {
        return carrierCode;
    }

    public void setCarrierCode(String carrierCode) {
        this.carrierCode = carrierCode;
    }

    public String getOriginWarehouseCode() {
        return originWarehouseCode;
    }

    public void setOriginWarehouseCode(String originWarehouseCode) {
        this.originWarehouseCode = originWarehouseCode;
    }

    public String getDestinationRegionCode() {
        return destinationRegionCode;
    }

    public void setDestinationRegionCode(String destinationRegionCode) {
        this.destinationRegionCode = destinationRegionCode;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public BigDecimal getBaseCost() {
        return baseCost;
    }

    public void setBaseCost(BigDecimal baseCost) {
        this.baseCost = baseCost;
    }

    public Integer getSlaHours() {
        return slaHours;
    }

    public void setSlaHours(Integer slaHours) {
        this.slaHours = slaHours;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
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
