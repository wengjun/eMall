package com.emall.aftersales.service;

import com.emall.aftersales.domain.AfterSalesRequest;
import com.emall.aftersales.domain.AfterSalesStatus;
import com.emall.aftersales.domain.AfterSalesType;
import com.emall.aftersales.repository.AfterSalesRepository;
import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AfterSalesService {
    private final AfterSalesRepository afterSalesRepository;
    private final SnowflakeIdGenerator idGenerator;

    public AfterSalesService(AfterSalesRepository afterSalesRepository, SnowflakeIdGenerator idGenerator) {
        this.afterSalesRepository = afterSalesRepository;
        this.idGenerator = idGenerator;
    }

    @Transactional
    public AfterSalesRequest create(long orderId, long userId, long skuId, int quantity,
                                    BigDecimal refundAmount, AfterSalesType type, String reason) {
        Instant now = Instant.now();
        return afterSalesRepository.save(new AfterSalesRequest(idGenerator.nextId(), orderId, userId, skuId,
                quantity, refundAmount, type, AfterSalesStatus.REQUESTED, reason, now, now));
    }

    public AfterSalesRequest get(long requestId) {
        return afterSalesRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "after-sales request not found"));
    }

    @Transactional
    public AfterSalesRequest approve(long requestId) {
        return move(requestId, AfterSalesStatus.REQUESTED, AfterSalesStatus.APPROVED);
    }

    @Transactional
    public AfterSalesRequest reject(long requestId) {
        return move(requestId, AfterSalesStatus.REQUESTED, AfterSalesStatus.REJECTED);
    }

    @Transactional
    public AfterSalesRequest receive(long requestId) {
        return move(requestId, AfterSalesStatus.APPROVED, AfterSalesStatus.RECEIVED);
    }

    @Transactional
    public AfterSalesRequest refund(long requestId) {
        AfterSalesRequest request = get(requestId);
        if (request.status() != AfterSalesStatus.APPROVED && request.status() != AfterSalesStatus.RECEIVED) {
            throw new BusinessException(ErrorCode.CONFLICT, "request cannot be refunded from " + request.status());
        }
        return afterSalesRepository.save(request.changeStatus(AfterSalesStatus.REFUNDED));
    }

    private AfterSalesRequest move(long requestId, AfterSalesStatus expected, AfterSalesStatus target) {
        AfterSalesRequest request = get(requestId);
        if (request.status() != expected) {
            throw new BusinessException(ErrorCode.CONFLICT, "request cannot move from " + request.status());
        }
        return afterSalesRepository.save(request.changeStatus(target));
    }
}
