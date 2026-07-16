package com.emall.order.repository;

import com.emall.common.outbox.OutboxEventMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderOutboxEventMapper extends OutboxEventMapper {
}
