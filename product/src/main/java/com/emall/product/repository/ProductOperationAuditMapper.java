package com.emall.product.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emall.common.operations.OperationAuditRecordEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductOperationAuditMapper extends BaseMapper<OperationAuditRecordEntity> {
}
