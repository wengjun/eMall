package com.emall.search.domain;

import java.time.Instant;
import java.util.List;

public record SearchResult(
        String keyword,
        List<SearchDocument> documents,
        int total,
        Instant searchedAt
) {
    public static SearchResult of(String keyword, List<SearchDocument> documents) {
        return new SearchResult(keyword, documents, documents.size(), Instant.now());
    }
}
