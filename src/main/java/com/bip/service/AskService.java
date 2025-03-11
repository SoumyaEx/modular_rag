package com.bip.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AskService {
    private final TransformQuery transformQuery;
    private final MultiQueryExpanderService queryExpanderService;
    private final RetrieverService retrieverService;
    private final DocumentJoinerService documentJoinerService;
    private final AugmentorService augmentorService;
    private final GeneratorService generatorService;

    public AskService(TransformQuery transformQuery, MultiQueryExpanderService queryExpanderService, RetrieverService retrieverService, DocumentJoinerService documentJoinerService, AugmentorService augmentorService, GeneratorService generatorService) {
        this.transformQuery = transformQuery;
        this.queryExpanderService = queryExpanderService;
        this.retrieverService = retrieverService;
        this.documentJoinerService = documentJoinerService;
        this.augmentorService = augmentorService;
        this.generatorService = generatorService;
    }


    public String generateResponse(String query) {
        String transformQry = transformQuery.transformQuery(query);
        List<String> expandedQueries = queryExpanderService.expandQuery(transformQry);

        Map<String, List<String>> retrievedDocsMap = expandedQueries.stream()
                .collect(Collectors.toMap(q -> q, retrieverService::retrieveDocuments));
        List<String> joinedDocuments = documentJoinerService.joinDocuments(retrievedDocsMap);
        String augmentedDocs = augmentorService.augmentDocuments(joinedDocuments,transformQry);

        return generatorService.generateResponse(augmentedDocs, query);
    }
}
