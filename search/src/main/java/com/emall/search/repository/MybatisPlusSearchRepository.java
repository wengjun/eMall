package com.emall.search.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.emall.search.domain.SearchDocument;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.search.engine", havingValue = "jdbc", matchIfMissing = true)
public class MybatisPlusSearchRepository implements SearchRepository {
    private final SearchDocumentMapper searchDocumentMapper;

    public MybatisPlusSearchRepository(SearchDocumentMapper searchDocumentMapper) {
        this.searchDocumentMapper = searchDocumentMapper;
    }

    @Override
    public SearchDocument save(SearchDocument document) {
        SearchDocumentEntity entity = toEntity(document);
        try {
            searchDocumentMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            searchDocumentMapper.update(null, new UpdateWrapper<SearchDocumentEntity>()
                    .set("title", entity.getTitle())
                    .set("category", entity.getCategory())
                    .set("price", entity.getPrice())
                    .set("tags", entity.getTags())
                    .set("saleable", entity.getSaleable())
                    .set("indexed_at", entity.getIndexedAt())
                    .eq("sku_id", entity.getSkuId()));
        }
        return document;
    }

    @Override
    public Optional<SearchDocument> findBySkuId(long skuId) {
        return Optional.ofNullable(searchDocumentMapper.selectById(skuId)).map(this::toDomain);
    }

    @Override
    public List<SearchDocument> search(String keyword, int limit) {
        String pattern = keyword == null ? "" : keyword;
        return searchDocumentMapper.selectList(new QueryWrapper<SearchDocumentEntity>()
                .eq("saleable", true)
                .and(query -> query.like("title", pattern).or().like("category", pattern).or().like("tags", pattern))
                .orderByDesc("indexed_at")
                .last("LIMIT " + limit)).stream().map(this::toDomain).toList();
    }

    @Override
    public void delete(long skuId) {
        searchDocumentMapper.deleteById(skuId);
    }

    private SearchDocumentEntity toEntity(SearchDocument document) {
        SearchDocumentEntity entity = new SearchDocumentEntity();
        entity.setSkuId(document.skuId());
        entity.setTitle(document.title());
        entity.setCategory(document.category());
        entity.setPrice(document.price());
        entity.setTags(serialize(document.tags()));
        entity.setSaleable(document.saleable());
        entity.setIndexedAt(LocalDateTime.ofInstant(document.indexedAt(), ZoneOffset.UTC));
        return entity;
    }

    private SearchDocument toDomain(SearchDocumentEntity entity) {
        return new SearchDocument(entity.getSkuId(), entity.getTitle(), entity.getCategory(), entity.getPrice(),
                deserialize(entity.getTags()), entity.getSaleable(), entity.getIndexedAt().toInstant(ZoneOffset.UTC));
    }

    private String serialize(Set<String> tags) {
        return tags == null ? "" : String.join(",", tags);
    }

    private Set<String> deserialize(String tags) {
        if (tags == null || tags.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(tags.split(",")).filter(tag -> !tag.isBlank()).collect(Collectors.toUnmodifiableSet());
    }
}
