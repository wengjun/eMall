package com.emall.search.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.search.domain.SearchDocument;
import com.emall.search.domain.SearchPage;
import com.emall.search.domain.SearchQuery;
import com.emall.search.service.SearchIndexLifecycleService;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.elasticsearch.DataElasticsearchTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.index.AliasAction;
import org.springframework.data.elasticsearch.core.index.AliasActionParameters;
import org.springframework.data.elasticsearch.core.index.AliasActions;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DataElasticsearchTest(properties = {"emall.search.engine=elasticsearch",
        "emall.search.elasticsearch.index-prefix=emall-search-document-v",
        "emall.search.elasticsearch.read-alias=emall-search-document-read",
        "emall.search.elasticsearch.write-alias=emall-search-document-write",
        "emall.search.elasticsearch.pit-keep-alive=2m",
        "emall.search.elasticsearch.cursor-secret=integration-search-cursor-secret-32-characters"})
@Import({ElasticsearchSearchRepository.class, ElasticsearchVersionedDocumentWriter.class, SearchCursorCodec.class,
        SearchIndexLifecycleService.class})
class ElasticsearchSearchRepositoryIT {
    private static final String BASELINE_INDEX = "emall-search-document-vbaseline";
    private static final String NEXT_INDEX = "emall-search-document-vnext";
    private static final String READ_ALIAS = "emall-search-document-read";
    private static final String WRITE_ALIAS = "emall-search-document-write";
    private static final int MATCHING_DOCUMENTS = 257;
    private static final int DOCUMENT_COUNT = Integer.getInteger("emall.search.it.document-count", 10_000);
    private static final Instant INDEX_TIME = Instant.parse("2026-07-15T08:00:00Z");

    @Container
    static final ElasticsearchContainer ELASTICSEARCH =
            new ElasticsearchContainer(DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.13.4"))
                    .withEnv("xpack.security.enabled", "false").withStartupTimeout(Duration.ofMinutes(3));

    @Autowired
    private ElasticsearchOperations operations;

    @Autowired
    private ElasticsearchDocumentRepository documentRepository;

    @Autowired
    private ElasticsearchSearchRepository searchRepository;

    @Autowired
    private SearchIndexLifecycleService lifecycleService;

    @DynamicPropertySource
    static void elasticsearchProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.uris", ELASTICSEARCH::getHttpHostAddress);
    }

    @BeforeAll
    void prepareBaselineIndex() {
        createIndex(BASELINE_INDEX);
        IndexOperations indexOperations = operations.indexOps(IndexCoordinates.of(BASELINE_INDEX));
        AliasActions aliases = new AliasActions(
                new AliasAction.Add(
                        AliasActionParameters.builder().withIndices(BASELINE_INDEX).withAliases(READ_ALIAS).build()),
                new AliasAction.Add(AliasActionParameters.builder().withIndices(BASELINE_INDEX).withAliases(WRITE_ALIAS)
                        .withIsWriteIndex(true).build()));
        assertThat(indexOperations.alias(aliases)).isTrue();
        indexDocuments();
    }

    @AfterAll
    void deleteIndices() {
        operations.indexOps(IndexCoordinates.of(BASELINE_INDEX)).delete();
        operations.indexOps(IndexCoordinates.of(NEXT_INDEX)).delete();
    }

    @Test
    void shouldSearchEveryMatchingDocumentAndSupportRebuildActivationAndRollback() {
        List<Long> foundSkuIds = new ArrayList<>();
        String cursor = null;
        do {
            SearchPage page = searchRepository.searchPage(new SearchQuery("needle", 37, cursor));
            assertThat(page.total()).isEqualTo(MATCHING_DOCUMENTS);
            foundSkuIds.addAll(page.documents().stream().map(SearchDocument::skuId).toList());
            cursor = page.nextCursor();
        } while (cursor != null);

        assertThat(foundSkuIds).hasSize(MATCHING_DOCUMENTS);
        assertThat(new HashSet<>(foundSkuIds)).hasSize(MATCHING_DOCUMENTS);
        assertThat(foundSkuIds).containsExactlyElementsOf(expectedSkuIds());

        createIndex(NEXT_INDEX);
        assertThat(lifecycleService.reindex("next", 100_000)).isEqualTo(DOCUMENT_COUNT + 1);
        assertThat(lifecycleService.verify("next", DOCUMENT_COUNT + 1L)).isEqualTo(DOCUMENT_COUNT + 1);
        assertThat(lifecycleService.activate("next", DOCUMENT_COUNT + 1L)).isEqualTo(DOCUMENT_COUNT + 1);
        assertThat(searchRepository.searchPage(new SearchQuery("needle", 37, null)).total())
                .isEqualTo(MATCHING_DOCUMENTS);

        assertThat(lifecycleService.activate("baseline", DOCUMENT_COUNT + 1L)).isEqualTo(DOCUMENT_COUNT + 1);
        assertThat(operations.indexOps(IndexCoordinates.of(BASELINE_INDEX)).getAliases(READ_ALIAS))
                .containsKey(BASELINE_INDEX);
    }

    @Test
    void shouldRejectOutOfOrderDocumentVersionsAtomically() {
        long skuId = 1_234_567L;
        SearchDocument current = new SearchDocument(skuId, "current phone", "digital", BigDecimal.TEN,
                Set.of("current"), true, 20L, INDEX_TIME);
        SearchDocument stale = new SearchDocument(skuId, "stale phone", "digital", BigDecimal.ONE, Set.of("stale"),
                true, 10L, INDEX_TIME.minusSeconds(1));

        assertThat(searchRepository.save(current)).isEqualTo(current);
        assertThat(searchRepository.save(stale)).isEqualTo(current);
        assertThat(searchRepository.findBySkuId(skuId)).contains(current);
    }

    private void indexDocuments() {
        int batchSize = 1_000;
        for (int offset = 0; offset < DOCUMENT_COUNT; offset += batchSize) {
            int end = Math.min(offset + batchSize, DOCUMENT_COUNT);
            List<ElasticsearchSearchDocument> batch = new ArrayList<>(end - offset);
            for (int index = offset; index < end; index++) {
                boolean matching = index < MATCHING_DOCUMENTS;
                batch.add(ElasticsearchSearchDocument.from(new SearchDocument(900_000L + index,
                        matching ? "needle product" : "ordinary product", "digital", BigDecimal.TEN,
                        Set.of(matching ? "needle" : "ordinary"), true, index + 1L, INDEX_TIME.minusSeconds(index))));
            }
            documentRepository.saveAll(batch);
        }
        documentRepository.save(ElasticsearchSearchDocument.from(new SearchDocument(999_999L, "needle hidden",
                "digital", BigDecimal.TEN, Set.of("needle"), false, 1L, INDEX_TIME.plusSeconds(1))));
        operations.indexOps(IndexCoordinates.of(BASELINE_INDEX)).refresh();
    }

    private void createIndex(String index) {
        IndexOperations target = operations.indexOps(IndexCoordinates.of(index));
        Map<String, Object> settings = Map.of("index.number_of_shards", 1, "index.number_of_replicas", 0,
                "index.analysis.analyzer.ik_max_word.type", "standard", "index.analysis.analyzer.ik_smart.type",
                "standard");
        assertThat(target.create(settings, operations.indexOps(ElasticsearchSearchDocument.class).createMapping()))
                .isTrue();
    }

    private List<Long> expectedSkuIds() {
        return java.util.stream.LongStream.range(900_000L, 900_000L + MATCHING_DOCUMENTS).boxed().toList();
    }
}
