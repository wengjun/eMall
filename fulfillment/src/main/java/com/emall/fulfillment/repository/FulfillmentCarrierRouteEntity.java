package com.emall.fulfillment.repository;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("fulfillment_carrier_route")
@Getter
@Setter
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

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
