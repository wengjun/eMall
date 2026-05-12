package com.emall.search.repository;

import com.emall.search.domain.SearchDocument;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.search.engine", havingValue = "elasticsearch")
public class ElasticsearchSearchRepository implements SearchRepository {
    private final ElasticsearchDocumentRepository repository;

    public ElasticsearchSearchRepository(ElasticsearchDocumentRepository repository) {
        this.repository = repository;
    }

    @Override
    public SearchDocument save(SearchDocument document) {
        return repository.save(ElasticsearchSearchDocument.from(document)).toDomain();
    }

    @Override
    public Optional<SearchDocument> findBySkuId(long skuId) {
        return repository.findById(String.valueOf(skuId)).map(ElasticsearchSearchDocument::toDomain);
    }

    @Override
    public List<SearchDocument> search(String keyword, int limit) {
        int pageSize = Math.max(limit, 1) * 5;
        return repository.findAll(PageRequest.of(0, pageSize)).stream().map(ElasticsearchSearchDocument::toDomain)
                .filter(SearchDocument::saleable).filter(document -> document.matches(keyword))
                .sorted(Comparator.comparing(SearchDocument::indexedAt).reversed()).limit(limit).toList();
    }

    @Override
    public void delete(long skuId) {
        repository.deleteById(String.valueOf(skuId));
    }
}
