package com.emall.search.domain;

import java.time.Instant;
import java.util.List;

public record SearchResult(String keyword, List<SearchDocument> documents, long total, String nextCursor,
        Instant searchedAt) {
    public static SearchResult of(String keyword, List<SearchDocument> documents) {
        return new SearchResult(keyword, documents, documents.size(), null, Instant.now());
    }

    public static SearchResult of(String keyword, SearchPage page) {
        return new SearchResult(keyword, page.documents(), page.total(), page.nextCursor(), Instant.now());
    }
}
