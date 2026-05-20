package com.emall.search.repository;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("search_document")
@Getter
@Setter
public class SearchDocumentEntity {
    @TableId(value = "sku_id", type = IdType.INPUT)
    private Long skuId;

    @TableField("title")
    private String title;

    @TableField("category")
    private String category;

    @TableField("price")
    private BigDecimal price;

    @TableField("tags")
    private String tags;

    @TableField("saleable")
    private Boolean saleable;

    @TableField("event_version")
    private Long version;

    @TableField("indexed_at")
    private LocalDateTime indexedAt;
}
