package com.emall.flashsale.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emall.common.outbox.OutboxEventRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FlashSaleOutboxEventMapper extends BaseMapper<OutboxEventRecord> {
}
