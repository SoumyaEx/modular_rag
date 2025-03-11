package com.bip.service;

import org.springframework.ai.chat.client.DefaultChatClientBuilder;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MultiQueryExpanderService {
    private final DefaultChatClientBuilder defaultChatClientBuilder;

    public MultiQueryExpanderService(DefaultChatClientBuilder defaultChatClientBuilder) {
        this.defaultChatClientBuilder = defaultChatClientBuilder;
    }

    public List<String> expandQuery(String query) {
        MultiQueryExpander queryExpander = MultiQueryExpander.builder()
                .chatClientBuilder(defaultChatClientBuilder)
                .numberOfQueries(3)
                .build();

        List<Query> queries = queryExpander.expand(new Query(query));
        return queries.stream()
                .map(Query::text)
                .collect(Collectors.toList());
    }
}