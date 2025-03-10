package com.bip.service;

import com.bip.entity.Document;
import com.bip.util.GeminiClient;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OpenSearchService {
    private final OpenSearchClient client;
    private final ChatClient chatClient;
    private final GeminiClient geminiClient;

    private static final String INDEX_NAME = "document";

    public OpenSearchService(OpenSearchClient client, ChatClient.Builder chatClientBuilder, GeminiClient geminiClient) {
        this.client = client;
        this.chatClient = chatClientBuilder.build();
        this.geminiClient = geminiClient;
    }

    // Index a document in OpenSearch
    public void addDocument(String id, String content) throws IOException {
        Document document = new Document(id, content);
        client.index(new IndexRequest.Builder<Document>()
                .index(INDEX_NAME)
                .id(id)
                .document(document)
                .build()
        );
    }

    // Search for relevant documents
    public List<String> searchDocuments(String query) throws IOException {
        SearchRequest searchRequest = new SearchRequest.Builder()
                .index(INDEX_NAME)
                .query(q -> q.match(m -> m.field("content").query(FieldValue.of(query))))
                .build();

        SearchResponse<Document> response = client.search(searchRequest, Document.class);
        List<String> results = new ArrayList<>();

        for (Hit<Document> hit : response.hits().hits()) {
            results.add(hit.source().getContent());
        }
        return results;
    }

    /*// Retrieve relevant documents and generate response using OpenAI
    public String generateAnswer(String query) throws IOException {
        List<String> retrievedDocs = searchDocuments(query);

        if (retrievedDocs.isEmpty()) {
            return "No relevant information found.";
        }

        String context = String.join("\n", retrievedDocs);
        String systemPrompt = """
                    You are an AI assistant that provides answers strictly based on the information available in the retrieved documents.
                    If the requested information is not found in the provided documents, respond with:
                    "The requested information is not found in the documents."

                    Retrieved Document(s):
                    {documents}
                    """;

        PromptTemplate promptTemplate = new PromptTemplate(systemPrompt, Map.of("documents", context));
        Message systemMsg = promptTemplate.createMessage();
        Prompt prompt = new Prompt(systemMsg, new UserMessage(query));
        return chatClient.prompt(prompt).call().content();
    }

    // Call OpenAI API for LLM response
    private String callOpenAI(String query, String context) throws IOException, ParseException {
        String prompt = "Context:" + context + " User Query: " + query + " Answer:";

        // Construct request body using a Map
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4o-mini");

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", "You are a helpful assistant."));
        messages.add(Map.of("role", "user", "content", prompt));
        requestBody.put("messages", messages);

        // Convert to JSON
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(requestBody);

        // Make HTTP request
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost request = new HttpPost("https://api.openai.com/v1/chat/completions");
        request.setHeader("Authorization", "Bearer " + openaiApiKey);
        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(json));

        CloseableHttpResponse response = httpClient.execute(request);
        String responseBody = EntityUtils.toString(response.getEntity());
        httpClient.close();

        // Parse the JSON response to extract the "content" field
        JsonNode rootNode = objectMapper.readTree(responseBody);
        return rootNode.path("choices").get(0).path("message").path("content").asText();
    }*/

    public String generateAnswerFromUrl(String query) throws IOException {
        List<String> retrievedDocs = searchDocuments(query);

        if (retrievedDocs.isEmpty()) {
            return "No relevant information found.";
        }

        String context = String.join("\n", retrievedDocs);
        String systemPrompt = """
                    You are an AI assistant that provides answers strictly based on the information available in the retrieved documents. 
                    If the requested information is not found in the provided documents, respond with: 
                    "The requested information is not found in the documents."
                    
                    Retrieved Document(s):  
                    {documents}
                    """;
        PromptTemplate promptTemplate = new PromptTemplate(systemPrompt, Map.of("documents", context));
        Message systemMsg = promptTemplate.createMessage();
        Prompt prompt = new Prompt(systemMsg, new UserMessage(query));
        return chatClient.prompt(prompt).call().content();
    }

    public void processAndStorePdf(MultipartFile file) throws IOException {
        String text = extractTextFromPdf(file);
        indexPdfContent(file.getOriginalFilename(), text);
    }

    private String extractTextFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            return pdfTextStripper.getText(document);
        }
    }

    private void indexPdfContent(String fileName, String content) throws IOException {
        Map<String, Object> document = Map.of("content", content);

        IndexRequest<Map<String, Object>> indexRequest = new IndexRequest.Builder<Map<String, Object>>()
                .index("pdf_index")
                .id(fileName)
                .document(document)
                .build();

        client.index(indexRequest);
    }


    public String generateResponse(String query) throws IOException {
        SearchRequest searchRequest = new SearchRequest.Builder()
                .index("pdf_index")
                .query(q -> q.match(m -> m.field("content").query(FieldValue.of(query))))
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

   /* public void processAndStoreExcel(MultipartFile file) throws IOException {
        String text = extractTextFromExcel(file);
        indexExcelContent(file.getOriginalFilename(), text);
    }

    private String extractTextFromExcel(MultipartFile file) throws IOException {
        StringBuilder extractedText = new StringBuilder();
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            for (Sheet sheet : workbook) {
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        extractedText.append(cell.toString()).append(" ");
                    }
                    extractedText.append("\n");
                }
            }
        } catch (OfficeXmlFileException e) {
            throw new IOException("Unsupported Excel format. Please upload a valid .xls or .xlsx file.", e);
        }
        return extractedText.toString();
    }

    private void indexExcelContent(String fileName, String content) throws IOException {
        Map<String, Object> document = Map.of("content", content);
        IndexRequest<Map<String, Object>> indexRequest = new IndexRequest.Builder<Map<String, Object>>()
                .index("excel_index")
                .id(fileName)
                .document(document)
                .build();
        client.index(indexRequest);
    }*/

    public String generateExcelResponse(String query) throws IOException {
        SearchRequest searchRequest = new SearchRequest.Builder()
                .index("excel_index")
                .query(q -> q.match(m -> m.field("content").query(FieldValue.of(query))))
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
