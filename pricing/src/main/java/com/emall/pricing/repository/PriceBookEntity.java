package com.emall.pricing.repository;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("price_book")
@Getter
@Setter
public class PriceBookEntity {
    @TableId(value = "sku_id", type = IdType.INPUT)
    private Long skuId;

    @TableField("list_price")
    private BigDecimal listPrice;

    @TableField("sale_price")
    private BigDecimal salePrice;

    @TableField("currency")
    private String currency;

    @Version
    @TableField("version")
    private Long version;

    @TableField("active")
    private Boolean active;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
