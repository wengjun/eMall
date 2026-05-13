package com.emall.order.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emall.common.outbox.OutboxEventRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderOutboxEventMapper extends BaseMapper<OutboxEventRecord> {
}
