package com.emall.search.repository;

import com.emall.search.domain.SearchDocument;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
public class InMemorySearchRepository implements SearchRepository {
    private final ConcurrentMap<Long, SearchDocument> documents = new ConcurrentHashMap<>();

    public InMemorySearchRepository() {
        save(new SearchDocument(10001L, "flagship phone", "digital", BigDecimal.valueOf(379900, 2),
                Set.of("phone", "mobile"), true, 0L, Instant.now()));
        save(new SearchDocument(10002L, "thin laptop", "computer", BigDecimal.valueOf(679900, 2),
                Set.of("laptop", "computer"), true, 0L, Instant.now()));
    }

    @Override
    public SearchDocument save(SearchDocument document) {
        AtomicBox<SearchDocument> saved = new AtomicBox<>();
        documents.compute(document.skuId(), (skuId, existing) -> {
            if (existing != null && existing.version() > document.version()) {
                saved.set(existing);
                return existing;
            }
            saved.set(document);
            return document;
        });
        return saved.value();
    }

    @Override
    public Optional<SearchDocument> findBySkuId(long skuId) {
        return Optional.ofNullable(documents.get(skuId));
    }

    @Override
    public List<SearchDocument> search(String keyword, int limit) {
        return documents.values().stream().filter(SearchDocument::saleable)
                .filter(document -> document.matches(keyword))
                .sorted(Comparator.comparing(SearchDocument::indexedAt).reversed()).limit(limit).toList();
    }

    @Override
    public void delete(long skuId) {
        documents.remove(skuId);
    }

    private static final class AtomicBox<T> {
        private T value;

        void set(T value) {
            this.value = value;
        }

        T value() {
            return value;
        }
    }
}
