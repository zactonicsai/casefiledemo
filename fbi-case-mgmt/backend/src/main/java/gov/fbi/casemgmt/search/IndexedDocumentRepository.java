package gov.fbi.casemgmt.search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface IndexedDocumentRepository
        extends ElasticsearchRepository<IndexedDocument, String> {
}
