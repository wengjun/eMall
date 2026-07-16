package com.emall.search.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emall.search.domain.SearchDocument;
import com.emall.search.domain.SearchPage;
import com.emall.search.domain.SearchQuery;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

class ElasticsearchSearchRepositoryTest {
    private static final Instant NOW = Instant.parse("2026-07-15T08:00:00Z");
    private static final String CURSOR_SECRET = "test-search-cursor-secret-with-32-characters";
    private final ElasticsearchDocumentRepository documentRepository = mock(ElasticsearchDocumentRepository.class);
    private final ElasticsearchOperations operations = mock(ElasticsearchOperations.class);
    private final ElasticsearchVersionedDocumentWriter documentWriter =
            mock(ElasticsearchVersionedDocumentWriter.class);
    private final SearchCursorCodec cursorCodec =
            new SearchCursorCodec(JsonMapper.builder().addModule(new JavaTimeModule()).build(), CURSOR_SECRET,
                    Clock.fixed(NOW, ZoneOffset.UTC));
    private final ElasticsearchSearchRepository repository = new ElasticsearchSearchRepository(documentRepository,
            operations, documentWriter, cursorCodec, "emall-search-document-read", Duration.ofMinutes(2));

    @BeforeEach
    void openPointInTime() {
        when(operations.openPointInTime(any(), any(), eq(false))).thenReturn("pit-1");
    }

    @Test
    void shouldSaveFindAndDeleteThroughElasticsearch() {
        SearchDocument phone = document(30001L, "flagship phone", NOW);
        when(documentWriter.write(phone)).thenReturn(ElasticsearchVersionedDocumentWriter.WriteResult.APPLIED);
        when(documentRepository.findById("30001")).thenReturn(Optional.of(ElasticsearchSearchDocument.from(phone)));

        SearchDocument saved = repository.save(phone);
        Optional<SearchDocument> found = repository.findBySkuId(30001L);
        repository.delete(30001L);

        assertThat(saved).isEqualTo(phone);
        assertThat(found).contains(phone);
        verify(documentRepository).deleteById("30001");
    }

    @Test
    void shouldReturnCurrentDocumentWhenElasticsearchRejectsAnOlderVersion() {
        SearchDocument stale = document(30001L, "stale phone", NOW.minusSeconds(1));
        SearchDocument current = document(30001L, "current phone", NOW);
        when(documentWriter.write(stale)).thenReturn(ElasticsearchVersionedDocumentWriter.WriteResult.REJECTED_STALE);
        when(documentRepository.findById("30001")).thenReturn(Optional.of(ElasticsearchSearchDocument.from(current)));

        assertThat(repository.save(stale)).isEqualTo(current);
    }

    @Test
    void shouldUsePitAndSearchAfterForStableDeepPagination() {
        SearchDocument firstDocument = document(30001L, "flagship phone", NOW);
        SearchDocument secondDocument = document(30002L, "camera phone", NOW.minusSeconds(1));
        SearchHit<ElasticsearchSearchDocument> firstHit = hit(firstDocument, List.of(9.5, 1_700L, 30001L));
        SearchHit<ElasticsearchSearchDocument> secondHit = hit(secondDocument, List.of(8.5, 1_699L, 30002L));
        SearchHits<ElasticsearchSearchDocument> firstHits = hits(firstHit, 2, "pit-2");
        SearchHits<ElasticsearchSearchDocument> secondHits = hits(secondHit, 2, "pit-3");
        when(operations.search(any(NativeQuery.class), eq(ElasticsearchSearchDocument.class))).thenReturn(firstHits,
                secondHits);

        SearchPage firstPage = repository.searchPage(new SearchQuery("phone", 1, null));
        SearchPage secondPage = repository.searchPage(new SearchQuery("phone", 1, firstPage.nextCursor()));

        assertThat(firstPage.documents()).containsExactly(firstDocument);
        assertThat(firstPage.total()).isEqualTo(2);
        assertThat(firstPage.nextCursor()).isNotBlank();
        assertThat(secondPage.documents()).containsExactly(secondDocument);
        assertThat(secondPage.nextCursor()).isNull();

        ArgumentCaptor<NativeQuery> queryCaptor = ArgumentCaptor.forClass(NativeQuery.class);
        verify(operations, org.mockito.Mockito.times(2)).search(queryCaptor.capture(),
                eq(ElasticsearchSearchDocument.class));
        NativeQuery firstQuery = queryCaptor.getAllValues().get(0);
        NativeQuery secondQuery = queryCaptor.getAllValues().get(1);
        assertThat(firstQuery.getPointInTime().id()).isEqualTo("pit-1");
        assertThat(firstQuery.getSearchAfter()).isNull();
        assertThat(firstQuery.getQuery()).isNotNull();
        assertThat(firstQuery.getFilter()).isNotNull();
        assertThat(secondQuery.getPointInTime().id()).isEqualTo("pit-2");
        assertThat(secondQuery.getSearchAfter()).containsExactly(9.5, 1_700, 30001);
        verify(operations).closePointInTime("pit-3");
        verify(operations, never()).closePointInTime("pit-1");
    }

    @SuppressWarnings("unchecked")
    private SearchHit<ElasticsearchSearchDocument> hit(SearchDocument document, List<Object> sortValues) {
        SearchHit<ElasticsearchSearchDocument> hit = mock(SearchHit.class);
        when(hit.getContent()).thenReturn(ElasticsearchSearchDocument.from(document));
        when(hit.getSortValues()).thenReturn(sortValues);
        return hit;
    }

    @SuppressWarnings("unchecked")
    private SearchHits<ElasticsearchSearchDocument> hits(SearchHit<ElasticsearchSearchDocument> hit, long total,
            String pointInTimeId) {
        SearchHits<ElasticsearchSearchDocument> hits = mock(SearchHits.class);
        when(hits.getSearchHits()).thenReturn(List.of(hit));
        when(hits.getSearchHit(0)).thenReturn(hit);
        when(hits.getTotalHits()).thenReturn(total);
        when(hits.getPointInTimeId()).thenReturn(pointInTimeId);
        return hits;
    }

    private SearchDocument document(long skuId, String title, Instant indexedAt) {
        return new SearchDocument(skuId, title, "digital", new BigDecimal("3799.00"), Set.of("phone", "hot"), true, 10L,
                indexedAt);
    }
}
