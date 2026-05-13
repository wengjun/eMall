package com.emall.product.repository;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("product")
@Getter
@Setter
public class ProductEntity {
    @TableId(value = "sku_id", type = IdType.INPUT)
    private Long skuId;

    @TableField("spu_id")
    private Long spuId;

    @TableField("title")
    private String title;

    @TableField("category")
    private String category;

    @TableField("price")
    private BigDecimal price;

    @TableField("status")
    private String status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
