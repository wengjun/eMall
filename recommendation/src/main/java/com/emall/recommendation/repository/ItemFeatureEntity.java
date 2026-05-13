package com.emall.recommendation.repository;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("recommendation_item_feature")
@Getter
@Setter
public class ItemFeatureEntity {
    @TableId(value = "sku_id", type = IdType.INPUT)
    private Long skuId;

    @TableField("category_code")
    private String categoryCode;

    @TableField("base_score")
    private BigDecimal baseScore;

    @TableField("popularity_score")
    private BigDecimal popularityScore;

    @TableField("active")
    private Boolean active;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
