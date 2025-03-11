package com.bip.service;

import com.bip.entity.Document;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RetrieverService {
    private final OpenSearchClient openSearchClient;
    private static final String INDEX_NAME = "excel";

    public RetrieverService(OpenSearchClient openSearchClient) {
        this.openSearchClient = openSearchClient;
    }

    public List<String> retrieveDocuments(String query) {
        try {
            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(INDEX_NAME)
                    .query(q -> q.match(m -> m.field("content").query(FieldValue.of(query)))));

            SearchResponse<Document> searchResponse = openSearchClient.search(searchRequest, Document.class);

            return searchResponse.hits().hits().stream()
                    .map(Hit::source)
                    .map(Document::getContent)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Error querying OpenSearch", e);
        }
    }
}
