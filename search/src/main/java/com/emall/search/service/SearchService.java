package com.emall.search.service;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.sharding.ShardRoutingOperations;
import com.emall.search.domain.SearchDocument;
import com.emall.search.domain.SearchResult;
import com.emall.search.domain.SearchPage;
import com.emall.search.domain.SearchQuery;
import com.emall.search.repository.SearchRepository;
import java.math.BigDecimal;
import java.time.Instant;
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
        return shardRoutingOperations.executeRead("search_document", skuId, () -> searchRepository.findBySkuId(skuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "search document not found")));
    }

    public SearchResult search(String keyword, int limit) {
        return search(keyword, limit, null);
    }

    public SearchResult search(String keyword, int limit, String cursor) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        if (!jdbcSearch) {
            SearchPage page = searchRepository.searchPage(new SearchQuery(keyword, safeLimit, cursor));
            return SearchResult.of(keyword, page);
        }
        if (cursor != null && !cursor.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "cursor is only supported by Elasticsearch search");
        }
        var documents = searchRepository.search(keyword, safeLimit);
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
