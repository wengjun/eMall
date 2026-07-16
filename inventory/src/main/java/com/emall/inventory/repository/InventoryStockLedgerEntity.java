package com.emall.inventory.repository;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("inventory_stock_ledger")
@Getter
@Setter
public class InventoryStockLedgerEntity {
    @TableId(value = "ledger_id", type = IdType.INPUT)
    private String ledgerId;

    @TableField("sku_id")
    private Long skuId;

    @TableField("request_id")
    private String requestId;

    @TableField("operation")
    private String operation;

    @TableField("bucket_no")
    private Integer bucketNo;

    @TableField("total_delta")
    private Long totalDelta;

    @TableField("reserved_delta")
    private Long reservedDelta;

    @TableField("sold_delta")
    private Long soldDelta;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
