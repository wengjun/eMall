package com.emall.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emall.search.repository.ElasticsearchSearchDocument;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.AliasActions;
import org.springframework.data.elasticsearch.core.index.AliasData;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.reindex.ReindexRequest;
import org.springframework.data.elasticsearch.core.reindex.ReindexResponse;

class SearchIndexLifecycleServiceTest {
    private static final String PREFIX = "emall-search-document-v";
    private static final String READ_ALIAS = "emall-search-document-read";
    private static final String WRITE_ALIAS = "emall-search-document-write";
    private final ElasticsearchOperations operations = mock(ElasticsearchOperations.class);
    private final SearchIndexLifecycleService service =
            new SearchIndexLifecycleService(operations, PREFIX, READ_ALIAS, WRITE_ALIAS);

    @Test
    void shouldCreateVersionedIndexFromEntityMapping() {
        String index = PREFIX + "2026.07.15";
        IndexOperations target = mock(IndexOperations.class);
        IndexOperations mappingOperations = mock(IndexOperations.class);
        Document mapping = mock(Document.class);
        when(operations.indexOps(IndexCoordinates.of(index))).thenReturn(target);
        when(operations.indexOps(ElasticsearchSearchDocument.class)).thenReturn(mappingOperations);
        when(mappingOperations.createMapping()).thenReturn(mapping);
        when(target.create(anyMap(), eq(mapping))).thenReturn(true);

        int created = service.prepare("2026.07.15", 24, 1);

        assertThat(created).isEqualTo(1);
        verify(target).create(anyMap(), eq(mapping));
    }

    @Test
    void shouldReindexWithThrottleAndRejectPartialResponses() {
        String index = PREFIX + "2026.07.15";
        IndexOperations target = mock(IndexOperations.class);
        IndexOperations aliasLookup = mock(IndexOperations.class);
        ReindexResponse response = mock(ReindexResponse.class);
        when(operations.indexOps(IndexCoordinates.of(index))).thenReturn(target);
        when(operations.indexOps(IndexCoordinates.of(READ_ALIAS))).thenReturn(aliasLookup);
        when(target.exists()).thenReturn(true);
        when(aliasLookup.getAliases(READ_ALIAS)).thenReturn(Map.of("old-index", Set.of()));
        when(operations.reindex(any(ReindexRequest.class))).thenReturn(response);
        when(response.getCreated()).thenReturn(25L);
        when(response.getFailures()).thenReturn(List.of());

        int affected = service.reindex("2026.07.15", 1_000);

        assertThat(affected).isEqualTo(25);
        ArgumentCaptor<ReindexRequest> requestCaptor = ArgumentCaptor.forClass(ReindexRequest.class);
        verify(operations).reindex(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getRequestsPerSecond()).isEqualTo(1_000);
    }

    @Test
    void shouldVerifyCountAndAtomicallySwitchBothAliases() {
        String index = PREFIX + "2026.07.15";
        IndexOperations target = mock(IndexOperations.class);
        AliasData readAlias = alias(READ_ALIAS);
        AliasData writeAlias = alias(WRITE_ALIAS);
        when(operations.indexOps(IndexCoordinates.of(index))).thenReturn(target);
        when(operations.count(any(), eq(ElasticsearchSearchDocument.class), eq(IndexCoordinates.of(index))))
                .thenReturn(25L);
        when(target.getAliases(READ_ALIAS, WRITE_ALIAS)).thenReturn(Map.of("old-index", Set.of(readAlias, writeAlias)));
        when(target.alias(any(AliasActions.class))).thenReturn(true);

        int activated = service.activate("2026.07.15", 25);

        assertThat(activated).isEqualTo(25);
        ArgumentCaptor<AliasActions> actionsCaptor = ArgumentCaptor.forClass(AliasActions.class);
        verify(target).alias(actionsCaptor.capture());
        assertThat(actionsCaptor.getValue().getActions()).hasSize(3);
    }

    @Test
    void shouldRejectUnsafeIndexVersionAndCountMismatch() {
        assertThatThrownBy(() -> service.prepare("../../orders", 24, 1)).isInstanceOf(IllegalArgumentException.class);

        String index = PREFIX + "2026.07.15";
        when(operations.count(any(), eq(ElasticsearchSearchDocument.class), eq(IndexCoordinates.of(index))))
                .thenReturn(24L);
        assertThatThrownBy(() -> service.verify("2026.07.15", 25)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("count mismatch");
    }

    private AliasData alias(String name) {
        AliasData alias = mock(AliasData.class);
        when(alias.getAlias()).thenReturn(name);
        return alias;
    }
}
