package com.emall.search.service;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.search.domain.SearchDocument;
import com.emall.search.domain.SearchResult;
import com.emall.search.repository.SearchRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchService {
    private final SearchRepository searchRepository;

    public SearchService(SearchRepository searchRepository) {
        this.searchRepository = searchRepository;
    }

    @Transactional
    public SearchDocument index(long skuId, String title, String category, BigDecimal price,
                                Set<String> tags, boolean saleable) {
        return searchRepository.save(new SearchDocument(skuId, title, category, price, tags, saleable, Instant.now()));
    }

    public SearchDocument get(long skuId) {
        return searchRepository.findBySkuId(skuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "search document not found"));
    }

    public SearchResult search(String keyword, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return SearchResult.of(keyword, searchRepository.search(keyword, safeLimit));
    }

    @Transactional
    public void delete(long skuId) {
        searchRepository.delete(skuId);
    }
}
