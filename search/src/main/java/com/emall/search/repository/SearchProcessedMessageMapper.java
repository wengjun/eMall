package com.emall.search.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emall.common.messaging.ProcessedMessageRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SearchProcessedMessageMapper extends BaseMapper<ProcessedMessageRecord> {
}
