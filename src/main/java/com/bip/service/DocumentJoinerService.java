package com.bip.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.join.ConcatenationDocumentJoiner;
import org.springframework.ai.rag.retrieval.join.DocumentJoiner;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DocumentJoinerService {
    public List<String> joinDocuments(Map<String, List<String>> documentsForQuery) {
        DocumentJoiner documentJoiner = new ConcatenationDocumentJoiner();

        // Convert Map<String, List<String>> to Map<Query, List<List<Document>>>
        Map<Query, List<List<Document>>> transformedDocs = documentsForQuery.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> new Query(entry.getKey()),
                        entry -> List.of(entry.getValue().stream()
                                .map(Document::new)
                                .collect(Collectors.toList()))
                ));

        List<Document> joinedDocuments = documentJoiner.join(transformedDocs);

        return joinedDocuments.stream()
                .map(Document::getText)
                .collect(Collectors.toList());
    }
}