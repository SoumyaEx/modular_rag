package com.bip.service;

import com.bip.entity.Document;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
@Service
public class DocumentService {
    private final OpenSearchClient client;
    private static final String INDEX_NAME = "document";

    public DocumentService(OpenSearchClient client) {
        this.client = client;
    }

    public void addDocument(String id, String content) throws IOException {
        Document document = new Document(id, content);
        client.index(new IndexRequest.Builder<Document>()
                .index(INDEX_NAME)
                .id(id)
                .document(document)
                .build()
        );
    }
}
