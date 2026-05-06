package com.emall.search.repository;

import com.emall.search.domain.SearchDocument;
import java.util.List;
import java.util.Optional;

public interface SearchRepository {
    SearchDocument save(SearchDocument document);

    Optional<SearchDocument> findBySkuId(long skuId);

    List<SearchDocument> search(String keyword, int limit);

    void delete(long skuId);
}
