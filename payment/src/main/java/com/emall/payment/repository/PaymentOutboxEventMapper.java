package com.emall.payment.repository;

import com.emall.common.outbox.OutboxEventMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentOutboxEventMapper extends OutboxEventMapper {
}
