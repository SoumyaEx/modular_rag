package com.bip.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AugmentorService {
    private final QueryAugmenter queryAugmenter;

    public AugmentorService() {
        this.queryAugmenter = ContextualQueryAugmenter.builder()
                .allowEmptyContext(true)
                .build();
    }

    public String augmentDocuments(List<String> documents, String query) {
        if (documents.isEmpty()) {
            return "The requested information is not found in the documents.";
        }
        Query queryObj = new Query(query);
        List<Document> documentList = documents.stream()
                .map(Document::new)
                .collect(Collectors.toList());

        Query augmentedQuery = queryAugmenter.augment(queryObj, documentList);
        return augmentedQuery.text();
    }
}
