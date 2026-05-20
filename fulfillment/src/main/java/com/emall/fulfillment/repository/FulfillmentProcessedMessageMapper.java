package com.emall.fulfillment.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emall.common.messaging.ProcessedMessageRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FulfillmentProcessedMessageMapper extends BaseMapper<ProcessedMessageRecord> {
}
