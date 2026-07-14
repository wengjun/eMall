package com.emall.order.saga;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderSagaMapper extends BaseMapper<OrderSagaEntity> {
}
