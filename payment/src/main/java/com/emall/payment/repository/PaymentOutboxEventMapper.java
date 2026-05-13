package com.emall.payment.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emall.common.outbox.OutboxEventRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentOutboxEventMapper extends BaseMapper<OutboxEventRecord> {
}
