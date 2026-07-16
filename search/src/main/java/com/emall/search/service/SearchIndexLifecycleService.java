package com.emall.search.service;

import com.emall.search.repository.ElasticsearchSearchDocument;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.index.AliasAction;
import org.springframework.data.elasticsearch.core.index.AliasActionParameters;
import org.springframework.data.elasticsearch.core.index.AliasActions;
import org.springframework.data.elasticsearch.core.index.AliasData;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.reindex.ReindexRequest;
import org.springframework.data.elasticsearch.core.reindex.ReindexResponse;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "emall.search.engine", havingValue = "elasticsearch")
public class SearchIndexLifecycleService {
    private static final Pattern VERSION_PATTERN = Pattern.compile("[a-z0-9][a-z0-9._-]{0,63}");
    private final ElasticsearchOperations operations;
    private final String indexPrefix;
    private final String readAlias;
    private final String writeAlias;

    public SearchIndexLifecycleService(ElasticsearchOperations operations,
            @Value("${emall.search.elasticsearch.index-prefix:emall-search-document-v}") String indexPrefix,
            @Value("${emall.search.elasticsearch.read-alias}") String readAlias,
            @Value("${emall.search.elasticsearch.write-alias}") String writeAlias) {
        this.operations = operations;
        this.indexPrefix = indexPrefix;
        this.readAlias = readAlias;
        this.writeAlias = writeAlias;
    }

    public int prepare(String version, int shards, int replicas) {
        if (shards < 1 || shards > 256 || replicas < 0 || replicas > 8) {
            throw new IllegalArgumentException("invalid Elasticsearch shard or replica count");
        }
        IndexOperations target = indexOperations(version);
        if (target.exists()) {
            return 0;
        }
        Map<String, Object> settings = Map.of("index.number_of_shards", shards, "index.number_of_replicas", replicas,
                "index.refresh_interval", "5s");
        IndexOperations mapping = operations.indexOps(ElasticsearchSearchDocument.class);
        if (!target.create(settings, mapping.createMapping())) {
            throw new IllegalStateException("Elasticsearch did not acknowledge index creation");
        }
        return 1;
    }

    public int reindex(String version, long requestsPerSecond) {
        if (requestsPerSecond < 1 || requestsPerSecond > 1_000_000) {
            throw new IllegalArgumentException("requestsPerSecond must be between 1 and 1000000");
        }
        String targetIndex = indexName(version);
        if (!operations.indexOps(IndexCoordinates.of(targetIndex)).exists()) {
            throw new IllegalStateException("target search index does not exist: " + targetIndex);
        }
        if (currentReadIndices().isEmpty()) {
            return 0;
        }
        ReindexRequest request =
                ReindexRequest.builder(IndexCoordinates.of(readAlias), IndexCoordinates.of(targetIndex))
                        .withConflicts(ReindexRequest.Conflicts.ABORT).withRequestsPerSecond(requestsPerSecond)
                        .withRefresh(true).withSlices(8).build();
        ReindexResponse response = operations.reindex(request);
        if (response.isTimedOut() || response.getVersionConflicts() > 0 || !response.getFailures().isEmpty()) {
            throw new IllegalStateException("Elasticsearch reindex did not complete cleanly");
        }
        return Math.toIntExact(Math.min(response.getCreated() + response.getUpdated(), Integer.MAX_VALUE));
    }

    public int verify(String version, long expectedCount) {
        long targetCount = count(indexName(version));
        long requiredCount = expectedCount >= 0 ? expectedCount : currentReadIndices().isEmpty() ? 0 : count(readAlias);
        if (targetCount != requiredCount) {
            throw new IllegalStateException(
                    "search index count mismatch: expected=" + requiredCount + ", actual=" + targetCount);
        }
        return Math.toIntExact(Math.min(targetCount, Integer.MAX_VALUE));
    }

    public int activate(String version, long expectedCount) {
        int verified = verify(version, expectedCount);
        String targetIndex = indexName(version);
        IndexOperations aliasOperations = operations.indexOps(IndexCoordinates.of(targetIndex));
        AliasActions actions = new AliasActions();
        Map<String, Set<AliasData>> currentAliases = aliasOperations.getAliases(readAlias, writeAlias);
        currentAliases.forEach((index, aliases) -> {
            Set<String> names = new LinkedHashSet<>();
            aliases.stream().map(AliasData::getAlias)
                    .filter(alias -> alias.equals(readAlias) || alias.equals(writeAlias)).forEach(names::add);
            if (!names.isEmpty()) {
                actions.add(new AliasAction.Remove(AliasActionParameters.builder().withIndices(index)
                        .withAliases(names.toArray(String[]::new)).build()));
            }
        });
        actions.add(
                new AliasAction.Add(
                        AliasActionParameters.builder().withIndices(targetIndex).withAliases(readAlias).build()),
                new AliasAction.Add(AliasActionParameters.builder().withIndices(targetIndex).withAliases(writeAlias)
                        .withIsWriteIndex(true).build()));
        if (!aliasOperations.alias(actions)) {
            throw new IllegalStateException("Elasticsearch did not acknowledge alias activation");
        }
        return verified;
    }

    String indexName(String version) {
        String normalized = version == null ? "" : version.strip().toLowerCase(Locale.ROOT);
        if (!VERSION_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("invalid search index version");
        }
        return indexPrefix + normalized;
    }

    private IndexOperations indexOperations(String version) {
        return operations.indexOps(IndexCoordinates.of(indexName(version)));
    }

    private Set<String> currentReadIndices() {
        return operations.indexOps(IndexCoordinates.of(readAlias)).getAliases(readAlias).keySet();
    }

    private long count(String index) {
        return operations.count(operations.matchAllQuery(), ElasticsearchSearchDocument.class,
                IndexCoordinates.of(index));
    }
}
