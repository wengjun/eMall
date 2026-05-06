package com.emall.search.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

public record SearchDocument(
        long skuId,
        String title,
        String category,
        BigDecimal price,
        Set<String> tags,
        boolean saleable,
        Instant indexedAt
) {
    public boolean matches(String keyword) {
        String normalized = keyword == null ? "" : keyword.toLowerCase();
        return normalized.isBlank()
                || title.toLowerCase().contains(normalized)
                || category.toLowerCase().contains(normalized)
                || tags.stream().anyMatch(tag -> tag.toLowerCase().contains(normalized));
    }
}
