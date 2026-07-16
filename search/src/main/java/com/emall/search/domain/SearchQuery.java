package com.emall.search.domain;

public record SearchQuery(String keyword, int pageSize, String cursor) {
    public SearchQuery {
        keyword = keyword == null ? "" : keyword.strip();
        if (pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("pageSize must be between 1 and 100");
        }
    }

    public boolean hasCursor() {
        return cursor != null && !cursor.isBlank();
    }
}
