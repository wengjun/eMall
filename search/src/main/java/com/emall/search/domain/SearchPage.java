package com.emall.search.domain;

import java.util.List;

public record SearchPage(List<SearchDocument> documents, long total, String nextCursor) {
    public SearchPage {
        documents = List.copyOf(documents);
    }
}
