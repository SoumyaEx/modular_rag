package com.bip.service;

import com.bip.util.GeminiClient;
import org.springframework.stereotype.Service;

@Service
public class GeneratorService {
    private final GeminiClient geminiClient;

    public GeneratorService(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    public String generateResponse(String context, String query) {
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
