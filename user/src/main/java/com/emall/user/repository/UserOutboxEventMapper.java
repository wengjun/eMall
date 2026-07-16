package com.emall.user.repository;

import com.emall.common.outbox.OutboxEventMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserOutboxEventMapper extends OutboxEventMapper {
}
