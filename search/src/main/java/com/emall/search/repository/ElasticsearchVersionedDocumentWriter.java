package com.emall.search.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.VersionType;
import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.search.domain.SearchDocument;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "emall.search.engine", havingValue = "elasticsearch")
class ElasticsearchVersionedDocumentWriter {
    private final ElasticsearchClient client;
    private final String writeAlias;

    ElasticsearchVersionedDocumentWriter(ElasticsearchClient client,
            @Value("${emall.search.elasticsearch.write-alias}") String writeAlias) {
        this.client = client;
        this.writeAlias = writeAlias;
    }

    WriteResult write(SearchDocument document) {
        if (document.version() <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "search document version must be positive");
        }
        try {
            client.index(request -> request.index(writeAlias).id(String.valueOf(document.skuId()))
                    .document(ElasticsearchSearchDocument.from(document)).version(document.version())
                    .versionType(VersionType.External));
            return WriteResult.APPLIED;
        } catch (ElasticsearchException exception) {
            if (exception.status() == 409) {
                return WriteResult.REJECTED_STALE;
            }
            throw exception;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write the search document", exception);
        }
    }

    enum WriteResult {
        APPLIED,
        REJECTED_STALE
    }
}
