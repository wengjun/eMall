package com.emall.pricing.service;

import com.emall.pricing.domain.PriceBook;

public record PriceBookCacheEntry(boolean present, PriceBook priceBook) {
    public static PriceBookCacheEntry hit(PriceBook priceBook) {
        return new PriceBookCacheEntry(true, priceBook);
    }

    public static PriceBookCacheEntry miss() {
        return new PriceBookCacheEntry(false, null);
    }
}
