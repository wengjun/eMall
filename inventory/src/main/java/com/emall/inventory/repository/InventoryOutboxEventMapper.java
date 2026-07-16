package com.emall.inventory.repository;

import com.emall.common.outbox.OutboxEventMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InventoryOutboxEventMapper extends OutboxEventMapper {
}
