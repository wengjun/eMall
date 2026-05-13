package com.emall.fulfillment.repository;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("fulfillment_order")
@Getter
@Setter
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
}
