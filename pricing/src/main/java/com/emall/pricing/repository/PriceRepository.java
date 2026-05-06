package com.emall.pricing.repository;

import com.emall.pricing.domain.PriceBook;
import java.util.Optional;

public interface PriceRepository {
    PriceBook save(PriceBook priceBook);

    Optional<PriceBook> findBySkuId(long skuId);
}
