package com.bip.service;

import org.springframework.ai.chat.client.DefaultChatClientBuilder;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.stereotype.Service;

@Service
public class TransformQuery {
    private final DefaultChatClientBuilder defaultChatClientBuilder;

    public TransformQuery(DefaultChatClientBuilder defaultChatClientBuilder) {
        this.defaultChatClientBuilder = defaultChatClientBuilder;
    }

    public String transformQuery(String query) {
        Query transformQuery = Query.builder()
                .text(query)
                .build();
        QueryTransformer queryTransformer = CompressionQueryTransformer.builder()
                .chatClientBuilder(defaultChatClientBuilder)
                .build();

        Query transformedQuery = queryTransformer.transform(transformQuery);
        return transformedQuery.text();
    }
}
