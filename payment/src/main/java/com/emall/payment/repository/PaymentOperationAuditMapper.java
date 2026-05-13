package com.emall.payment.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emall.common.operations.OperationAuditRecordEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentOperationAuditMapper extends BaseMapper<OperationAuditRecordEntity> {
}
