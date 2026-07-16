package com.emall.inventory.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface InventoryBucketMapper extends BaseMapper<InventoryBucketEntity> {
    @Select("""
            SELECT COALESCE(SUM(total), 0) AS total,
                   COALESCE(SUM(reserved), 0) AS reserved,
                   COALESCE(SUM(sold), 0) AS sold,
                   COUNT(*) AS bucket_count,
                   MAX(updated_at) AS updated_at
              FROM inventory_bucket
             WHERE sku_id = #{skuId}
            """)
    InventoryBucketSummaryProjection summarize(@Param("skuId") long skuId);
}
