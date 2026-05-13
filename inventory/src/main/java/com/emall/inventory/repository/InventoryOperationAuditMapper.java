package com.emall.inventory.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emall.common.operations.OperationAuditRecordEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InventoryOperationAuditMapper extends BaseMapper<OperationAuditRecordEntity> {
}
