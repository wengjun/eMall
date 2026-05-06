package com.emall.pricing.service;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.pricing.domain.PriceBook;
import com.emall.pricing.domain.PriceQuote;
import com.emall.pricing.repository.PriceRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PricingService {
    private final PriceRepository priceRepository;

    public PricingService(PriceRepository priceRepository) {
        this.priceRepository = priceRepository;
    }

    @CachePut(value = "prices", key = "#skuId")
    @Transactional
    public PriceBook upsert(long skuId, BigDecimal listPrice, BigDecimal salePrice, String currency, boolean active) {
        validatePrice(listPrice, salePrice);
        PriceBook existing = priceRepository.findBySkuId(skuId).orElse(null);
        if (existing == null) {
            PriceBook created = new PriceBook(skuId, listPrice, salePrice, currency, 1L, active, Instant.now());
            return priceRepository.save(created);
        }
        return priceRepository.save(existing.change(listPrice, salePrice, currency, active));
    }

    @Cacheable(value = "prices", key = "#skuId")
    public PriceBook get(long skuId) {
        return priceRepository.findBySkuId(skuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "price not found"));
    }

    public PriceQuote quote(long skuId, int quantity) {
        PriceBook priceBook = get(skuId);
        if (!priceBook.active()) {
            throw new BusinessException(ErrorCode.CONFLICT, "price is inactive");
        }
        return PriceQuote.of(priceBook, quantity);
    }

    private void validatePrice(BigDecimal listPrice, BigDecimal salePrice) {
        if (listPrice.signum() <= 0 || salePrice.signum() <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "prices must be positive");
        }
        if (salePrice.compareTo(listPrice) > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "sale price cannot exceed list price");
        }
    }
}
