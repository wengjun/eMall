package com.emall.review.repository;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("product_review")
@Getter
@Setter
public class ProductReviewEntity {
    @TableId(value = "review_id", type = IdType.INPUT)
    private Long reviewId;

    @TableField("order_id")
    private Long orderId;

    @TableField("sku_id")
    private Long skuId;

    @TableField("user_id")
    private Long userId;

    @TableField("rating")
    private Integer rating;

    @TableField("content")
    private String content;

    @TableField("status")
    private String status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
