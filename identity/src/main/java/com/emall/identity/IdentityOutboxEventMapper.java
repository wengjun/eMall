package com.emall.identity;

import com.emall.common.outbox.OutboxEventMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
interface IdentityOutboxEventMapper extends OutboxEventMapper {
}
