package com.bip.controller;

import com.bip.service.*;
import org.apache.hc.core5.http.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/open-search")
public class OpenSearchController {
    private final OpenSearchService openSearchService;
    private final PdfService pdfService;
    private final DocumentService documentService;
    private final AskService askService;
    private final ExcelService excelService;

    public OpenSearchController(OpenSearchService openSearchService, PdfService pdfService, DocumentService documentService, AskService askService, ExcelService excelService) {
        this.openSearchService = openSearchService;
        this.pdfService = pdfService;
        this.documentService = documentService;
        this.askService = askService;
        this.excelService = excelService;
    }
    @PostMapping("/add")
    public String addDocument(@RequestBody Map<String, String> request) throws IOException {
        openSearchService.addDocument(request.get("id"), request.get("content"));
        return "Document indexed successfully!";
    }

    @GetMapping("/ask")
    public String ask(@RequestParam String query) throws IOException, ParseException {
        return askService.generateResponse(query);
    }

    @PostMapping("/add-url")
    public String addDocumentFromUrl(@RequestBody Map<String, String> request) throws IOException {
        String url = request.get("url");
        String content = fetchDocumentContent(url);

        String id = String.valueOf(url.hashCode());
        documentService.addDocument(id, content);

        return "Document indexed successfully from URL: " + url;
    }

    public String fetchDocumentContent(String url) throws IOException {
        Document doc = Jsoup.connect(url).get();
        return doc.text();
    }

    @PostMapping("/upload-pdf")
    public String uploadPdf(@RequestParam("file") MultipartFile file) throws IOException {
        pdfService.processAndStorePdf(file);
        return "PDF uploaded and indexed successfully";
    }

    @PostMapping("/upload-excel")
    public String uploadExcel(@RequestParam("file") MultipartFile file) throws IOException {
        excelService.processAndStoreExcel(file);
        return "Excel file uploaded and indexed successfully";
    }

}
