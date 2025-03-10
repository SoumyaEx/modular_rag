package com.bip.service;

import org.apache.poi.poifs.filesystem.OfficeXmlFileException;
import org.apache.poi.ss.usermodel.*;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Service
public class ExcelService {
    private final OpenSearchClient client;
    private static final String INDEX_NAME = "document";

    public ExcelService(OpenSearchClient client) {
        this.client = client;
    }

    public void processAndStoreExcel(MultipartFile file) throws IOException {
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
                .index(INDEX_NAME)
                .id(fileName)
                .document(document)
                .build();
        client.index(indexRequest);
    }
}
