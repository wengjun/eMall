package com.emall.aftersales.repository;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.emall.aftersales.domain.AfterSalesRequest;
import com.emall.aftersales.domain.AfterSalesStatus;
import com.emall.aftersales.domain.AfterSalesType;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
public class MybatisPlusAfterSalesRepository implements AfterSalesRepository {
    private final AfterSalesRequestMapper afterSalesRequestMapper;

    public MybatisPlusAfterSalesRepository(AfterSalesRequestMapper afterSalesRequestMapper) {
        this.afterSalesRequestMapper = afterSalesRequestMapper;
    }

    @Override
    public AfterSalesRequest save(AfterSalesRequest request) {
        AfterSalesRequestEntity entity = toEntity(request);
        try {
            afterSalesRequestMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            afterSalesRequestMapper.update(null,
                    new UpdateWrapper<AfterSalesRequestEntity>().set("status", entity.getStatus())
                            .set("reason", entity.getReason()).set("updated_at", entity.getUpdatedAt())
                            .eq("request_id", entity.getRequestId()));
        }
        return request;
    }

    @Override
    public Optional<AfterSalesRequest> findById(long requestId) {
        return Optional.ofNullable(afterSalesRequestMapper.selectById(requestId)).map(this::toDomain);
    }

    private AfterSalesRequestEntity toEntity(AfterSalesRequest request) {
        AfterSalesRequestEntity entity = new AfterSalesRequestEntity();
        entity.setRequestId(request.requestId());
        entity.setOrderId(request.orderId());
        entity.setUserId(request.userId());
        entity.setSkuId(request.skuId());
        entity.setQuantity(request.quantity());
        entity.setRefundAmount(request.refundAmount());
        entity.setType(request.type().name());
        entity.setStatus(request.status().name());
        entity.setReason(request.reason());
        entity.setCreatedAt(LocalDateTime.ofInstant(request.createdAt(), ZoneOffset.UTC));
        entity.setUpdatedAt(LocalDateTime.ofInstant(request.updatedAt(), ZoneOffset.UTC));
        return entity;
    }

    private AfterSalesRequest toDomain(AfterSalesRequestEntity entity) {
        return new AfterSalesRequest(entity.getRequestId(), entity.getOrderId(), entity.getUserId(), entity.getSkuId(),
                entity.getQuantity(), entity.getRefundAmount(), AfterSalesType.valueOf(entity.getType()),
                AfterSalesStatus.valueOf(entity.getStatus()), entity.getReason(),
                entity.getCreatedAt().toInstant(ZoneOffset.UTC), entity.getUpdatedAt().toInstant(ZoneOffset.UTC));
    }
}
