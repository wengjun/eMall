package com.emall.search.repository;

import com.emall.search.domain.SearchDocument;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "emall-search-document")
class ElasticsearchSearchDocument {
    @Id
    private String id;
    @Field(type = FieldType.Long)
    private long skuId;
    @Field(type = FieldType.Text)
    private String title;
    @Field(type = FieldType.Keyword)
    private String category;
    @Field(type = FieldType.Double)
    private BigDecimal price;
    @Field(type = FieldType.Keyword)
    private Set<String> tags;
    @Field(type = FieldType.Boolean)
    private boolean saleable;
    @Field(type = FieldType.Date)
    private Instant indexedAt;

    ElasticsearchSearchDocument() {
    }

    private ElasticsearchSearchDocument(SearchDocument document) {
        this.id = String.valueOf(document.skuId());
        this.skuId = document.skuId();
        this.title = document.title();
        this.category = document.category();
        this.price = document.price();
        this.tags = document.tags();
        this.saleable = document.saleable();
        this.indexedAt = document.indexedAt();
    }

    static ElasticsearchSearchDocument from(SearchDocument document) {
        return new ElasticsearchSearchDocument(document);
    }

    SearchDocument toDomain() {
        return new SearchDocument(skuId, title, category, price, tags == null ? Set.of() : tags, saleable, indexedAt);
    }

    public String getId() {
        return id;
    }

    public long getSkuId() {
        return skuId;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Set<String> getTags() {
        return tags;
    }

    public boolean isSaleable() {
        return saleable;
    }

    public Instant getIndexedAt() {
        return indexedAt;
    }
}
