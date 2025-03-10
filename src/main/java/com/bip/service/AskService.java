package com.bip.service;

import com.bip.util.GeminiClient;
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
public class AskService {
    private final OpenSearchClient client;

    private final TransformQuery transformQuery;
    private final GeminiClient geminiClient;
    private static final String INDEX_NAME = "document";

    public AskService(OpenSearchClient client, TransformQuery transformQuery, GeminiClient geminiClient) {
        this.client = client;
        this.transformQuery = transformQuery;
        this.geminiClient = geminiClient;
    }


    public String generateResponse(String query) throws IOException {
        String transformQry = transformQuery.transformQuery(query);

        SearchRequest searchRequest = new SearchRequest.Builder()
                .index(INDEX_NAME)
                .query(q -> q.match(m -> m.field("content").query(FieldValue.of(transformQry))))
                .build();

        SearchResponse<Object> response = client.search(searchRequest, Object.class);
        List<String> retrievedDocs = response.hits().hits().stream()
                .map(Hit::source)
                .map(Object::toString)
                .collect(Collectors.toList());

        if (retrievedDocs.isEmpty()) {
            return "The requested information is not found in the documents.";
        }

        String context = String.join("\n", retrievedDocs);
        String systemPrompt = """
        You are an AI assistant that provides answers strictly based on the information available in the retrieved documents. 
        Your response should be concise and informative. You can use bullet points if necessary.
        If the requested information is not found in the provided documents, respond with: 
        "The requested information is not found in the documents."

        Retrieved Document(s):  
        {documents}
        User Query: {query}
    """;
        String finalPrompt = systemPrompt.replace("{documents}", context).replace("{query}", query);
        return geminiClient.generateResponse(finalPrompt);
    }
}
