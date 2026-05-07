package com.emall.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.exception.BusinessException;
import com.emall.search.domain.SearchResult;
import com.emall.search.repository.InMemorySearchRepository;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SearchServiceTest {
    private final SearchService searchService = new SearchService(new InMemorySearchRepository());

    @Test
    void shouldIndexSearchAndDeleteDocument() {
        searchService.index(30001L, "flagship phone", "digital", new BigDecimal("3799.00"), Set.of("phone", "hot"),
                true);

        SearchResult result = searchService.search("phone", 10);
        searchService.delete(30001L);

        assertThat(result.total()).isGreaterThanOrEqualTo(1);
        assertThat(result.documents()).filteredOn(document -> document.skuId() == 30001L).singleElement()
                .satisfies(document -> assertThat(document.saleable()).isTrue());
        assertThatThrownBy(() -> searchService.get(30001L)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("search document not found");
    }
}
