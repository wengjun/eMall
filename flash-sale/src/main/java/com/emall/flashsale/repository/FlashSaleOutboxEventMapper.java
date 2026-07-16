package com.emall.flashsale.repository;

import com.emall.common.outbox.OutboxEventMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FlashSaleOutboxEventMapper extends OutboxEventMapper {
}
