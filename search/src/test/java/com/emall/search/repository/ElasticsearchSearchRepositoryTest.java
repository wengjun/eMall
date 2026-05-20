package com.emall.search.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emall.search.domain.SearchDocument;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class ElasticsearchSearchRepositoryTest {
    private final ElasticsearchDocumentRepository documentRepository =
            org.mockito.Mockito.mock(ElasticsearchDocumentRepository.class);
    private final ElasticsearchSearchRepository repository = new ElasticsearchSearchRepository(documentRepository);

    @Test
    void shouldSaveFindSearchAndDeleteThroughElasticsearch() {
        SearchDocument phone = new SearchDocument(30001L, "flagship phone", "digital", new BigDecimal("3799.00"),
                Set.of("phone", "hot"), true, 10L, Instant.now());
        SearchDocument hidden = new SearchDocument(30002L, "hidden phone", "digital", new BigDecimal("2999.00"),
                Set.of("phone"), false, 11L, Instant.now());
        when(documentRepository.save(org.mockito.ArgumentMatchers.any(ElasticsearchSearchDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(documentRepository.findById("30001")).thenReturn(Optional.of(ElasticsearchSearchDocument.from(phone)));
        List<ElasticsearchSearchDocument> documents =
                List.of(ElasticsearchSearchDocument.from(phone), ElasticsearchSearchDocument.from(hidden));
        when(documentRepository.findAll(PageRequest.of(0, 10))).thenReturn(new PageImpl<>(documents));

        SearchDocument saved = repository.save(phone);
        Optional<SearchDocument> found = repository.findBySkuId(30001L);
        List<SearchDocument> result = repository.search("phone", 2);
        repository.delete(30001L);

        assertThat(saved.skuId()).isEqualTo(30001L);
        assertThat(found).contains(phone);
        assertThat(result).containsExactly(phone);
        verify(documentRepository).deleteById("30001");
    }
}
