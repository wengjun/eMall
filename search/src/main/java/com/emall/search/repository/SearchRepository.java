package com.emall.search.repository;

import com.emall.search.domain.SearchDocument;
import com.emall.search.domain.SearchPage;
import com.emall.search.domain.SearchQuery;
import java.util.List;
import java.util.Optional;

public interface SearchRepository {
    SearchDocument save(SearchDocument document);

    Optional<SearchDocument> findBySkuId(long skuId);

    List<SearchDocument> search(String keyword, int limit);

    default SearchPage searchPage(SearchQuery query) {
        if (query.hasCursor()) {
            throw new IllegalArgumentException("cursor pagination is not supported by this search engine");
        }
        List<SearchDocument> documents = search(query.keyword(), query.pageSize());
        return new SearchPage(documents, documents.size(), null);
    }

    void delete(long skuId);
}
