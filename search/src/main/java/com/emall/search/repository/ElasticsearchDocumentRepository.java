package com.emall.search.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

interface ElasticsearchDocumentRepository extends ElasticsearchRepository<ElasticsearchSearchDocument, String> {
}
