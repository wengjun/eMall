package com.emall.search.repository;

import co.elastic.clients.elasticsearch._types.SortOrder;
import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.search.domain.SearchDocument;
import com.emall.search.domain.SearchPage;
import com.emall.search.domain.SearchQuery;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.search.engine", havingValue = "elasticsearch")
public class ElasticsearchSearchRepository implements SearchRepository {
    private final ElasticsearchDocumentRepository repository;
    private final ElasticsearchOperations operations;
    private final ElasticsearchVersionedDocumentWriter documentWriter;
    private final SearchCursorCodec cursorCodec;
    private final String readAlias;
    private final Duration pitKeepAlive;

    @Autowired
    public ElasticsearchSearchRepository(ElasticsearchDocumentRepository repository, ElasticsearchOperations operations,
            ElasticsearchVersionedDocumentWriter documentWriter, SearchCursorCodec cursorCodec,
            @Value("${emall.search.elasticsearch.read-alias}") String readAlias,
            @Value("${emall.search.elasticsearch.pit-keep-alive:2m}") Duration pitKeepAlive) {
        this.repository = repository;
        this.operations = operations;
        this.documentWriter = documentWriter;
        this.cursorCodec = cursorCodec;
        this.readAlias = readAlias;
        this.pitKeepAlive = pitKeepAlive;
    }

    @Override
    public SearchDocument save(SearchDocument document) {
        if (documentWriter.write(document) == ElasticsearchVersionedDocumentWriter.WriteResult.APPLIED) {
            return document;
        }
        return findBySkuId(document.skuId())
                .orElseThrow(() -> new IllegalStateException("Rejected search document version is not readable"));
    }

    @Override
    public Optional<SearchDocument> findBySkuId(long skuId) {
        return repository.findById(String.valueOf(skuId)).map(ElasticsearchSearchDocument::toDomain);
    }

    @Override
    public List<SearchDocument> search(String keyword, int limit) {
        return searchPage(new SearchQuery(keyword, limit, null)).documents();
    }

    @Override
    public SearchPage searchPage(SearchQuery request) {
        String fingerprint = cursorCodec.fingerprint(request.keyword(), request.pageSize());
        SearchCursorState cursor = request.hasCursor() ? cursorCodec.decode(request.cursor()) : null;
        if (cursor != null && !fingerprint.equals(cursor.queryFingerprint())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "search cursor does not match the query");
        }

        boolean openedPointInTime = cursor == null;
        String pointInTimeId = openedPointInTime
                ? operations.openPointInTime(IndexCoordinates.of(readAlias), pitKeepAlive, false)
                : cursor.pitId();
        try {
            NativeQuery query = nativeQuery(request, pointInTimeId, cursor);
            SearchHits<ElasticsearchSearchDocument> hits = operations.search(query, ElasticsearchSearchDocument.class);
            List<SearchDocument> documents = hits.getSearchHits().stream().map(SearchHit::getContent)
                    .map(ElasticsearchSearchDocument::toDomain).toList();
            long seen = (cursor == null ? 0 : cursor.seen()) + documents.size();
            String responsePointInTimeId = hits.getPointInTimeId() == null ? pointInTimeId : hits.getPointInTimeId();
            if (documents.isEmpty() || documents.size() < request.pageSize() || seen >= hits.getTotalHits()) {
                operations.closePointInTime(responsePointInTimeId);
                return new SearchPage(documents, hits.getTotalHits(), null);
            }
            List<Object> sortValues = hits.getSearchHit(documents.size() - 1).getSortValues();
            if (sortValues == null || sortValues.isEmpty()) {
                operations.closePointInTime(responsePointInTimeId);
                throw new IllegalStateException("Elasticsearch returned a full page without sort values");
            }
            SearchCursorState next = new SearchCursorState(responsePointInTimeId, sortValues, fingerprint, seen,
                    Instant.now().plus(pitKeepAlive));
            return new SearchPage(documents, hits.getTotalHits(), cursorCodec.encode(next));
        } catch (RuntimeException ex) {
            if (openedPointInTime) {
                operations.closePointInTime(pointInTimeId);
            }
            throw ex;
        }
    }

    @Override
    public void delete(long skuId) {
        repository.deleteById(String.valueOf(skuId));
    }

    private NativeQuery nativeQuery(SearchQuery request, String pointInTimeId, SearchCursorState cursor) {
        NativeQueryBuilder builder =
                NativeQuery.builder().withFilter(query -> query.term(term -> term.field("saleable").value(true)))
                        .withPageable(PageRequest.of(0, request.pageSize())).withTrackTotalHits(true);
        if (request.keyword().isBlank()) {
            builder.withQuery(query -> query.matchAll(matchAll -> matchAll));
        } else {
            builder.withQuery(query -> query.multiMatch(multiMatch -> multiMatch.query(request.keyword())
                    .fields("title^5", "tags^3", "category^2").fuzziness("AUTO")));
            builder.withSort(sort -> sort.score(score -> score.order(SortOrder.Desc)));
        }
        builder.withSort(sort -> sort.field(field -> field.field("indexedAt").order(SortOrder.Desc)))
                .withSort(sort -> sort.field(field -> field.field("skuId").order(SortOrder.Asc)));
        NativeQuery query = builder.build();
        query.setPointInTime(new Query.PointInTime(pointInTimeId, pitKeepAlive));
        if (cursor != null) {
            query.setSearchAfter(cursor.sortValues());
        }
        return query;
    }
}
