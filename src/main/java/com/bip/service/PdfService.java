package com.bip.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
@Service
public class PdfService {
    private final OpenSearchClient client;
    private static final String INDEX_NAME = "document";
    public PdfService(OpenSearchClient client) {
        this.client = client;
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
                .index(INDEX_NAME)
                .id(fileName)
                .document(document)
                .build();

        client.index(indexRequest);
    }
}
