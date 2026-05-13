package com.emall.fulfillment.repository;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("fulfillment_warehouse")
@Getter
@Setter
public class FulfillmentWarehouseEntity {
    @TableId(value = "warehouse_code", type = IdType.INPUT)
    private String warehouseCode;

    @TableField("region_code")
    private String regionCode;

    @TableField("priority")
    private Integer priority;

    @TableField("daily_capacity")
    private Integer dailyCapacity;

    @TableField("enabled")
    private Boolean enabled;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
