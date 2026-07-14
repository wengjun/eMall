package com.emall.search.service;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.sharding.ShardRoutingOperations;
import com.emall.search.domain.SearchDocument;
import com.emall.search.domain.SearchResult;
import com.emall.search.repository.SearchRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchService {
    private final SearchRepository searchRepository;
    private final ShardRoutingOperations shardRoutingOperations;
    private final boolean jdbcSearch;

    public SearchService(SearchRepository searchRepository) {
        this(searchRepository, ShardRoutingOperations.noop(), "memory");
    }

    @Autowired
    public SearchService(SearchRepository searchRepository, ShardRoutingOperations shardRoutingOperations,
            @Value("${emall.search.engine:elasticsearch}") String searchEngine) {
        this.searchRepository = searchRepository;
        this.shardRoutingOperations = shardRoutingOperations;
        this.jdbcSearch = "jdbc".equalsIgnoreCase(searchEngine);
    }

    @Transactional
    public SearchDocument index(long skuId, String title, String category, BigDecimal price, Set<String> tags,
            boolean saleable) {
        return index(skuId, title, category, price, tags, saleable, Instant.now().toEpochMilli());
    }

    @Transactional
    public SearchDocument index(long skuId, String title, String category, BigDecimal price, Set<String> tags,
            boolean saleable, long version) {
        return shardRoutingOperations.execute("search_document", skuId, () -> searchRepository
                .save(new SearchDocument(skuId, title, category, price, tags, saleable, version, Instant.now())));
    }

    public SearchDocument get(long skuId) {
        return shardRoutingOperations.execute("search_document", skuId, () -> searchRepository.findBySkuId(skuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "search document not found")));
    }

    public SearchResult search(String keyword, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        List<SearchDocument> documents = jdbcSearch
                ? shardRoutingOperations
                        .executeAll("search_document", () -> searchRepository.search(keyword, safeLimit)).stream()
                        .flatMap(List::stream).sorted(Comparator.comparing(SearchDocument::indexedAt).reversed())
                        .limit(safeLimit).toList()
                : searchRepository.search(keyword, safeLimit);
        return SearchResult.of(keyword, documents);
    }

    @Transactional
    public void delete(long skuId) {
        shardRoutingOperations.execute("search_document", skuId, () -> {
            searchRepository.delete(skuId);
            return null;
        });
    }
}
