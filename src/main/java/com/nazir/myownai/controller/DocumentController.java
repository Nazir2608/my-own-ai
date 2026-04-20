package com.nazir.myownai.controller;

import com.nazir.myownai.dto.AskQuestionRequest;
import com.nazir.myownai.dto.AskResponse;
import com.nazir.myownai.dto.DocumentResponse;
import com.nazir.myownai.dto.InsertDocumentRequest;
import com.nazir.myownai.dto.SearchContextResponse;
import com.nazir.myownai.model.DocItem;
import com.nazir.myownai.service.DocumentDBService;
import com.nazir.myownai.service.FileParserService;
import com.nazir.myownai.service.OllamaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class DocumentController {

    private final DocumentDBService documentDBService;
    private final OllamaService ollamaService;
    private final FileParserService fileParserService;

    public DocumentController(DocumentDBService documentDBService, OllamaService ollamaService, FileParserService fileParserService) {
        this.documentDBService = documentDBService;
        this.ollamaService = ollamaService;
        this.fileParserService = fileParserService;
    }

    @PostMapping("/doc/insert")
    public ResponseEntity<?> insertDocument(@RequestBody InsertDocumentRequest request) {
        String title = request.getTitle();
        String text = request.getText();
        if (title == null || title.isBlank() || text == null || text.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Title and text are required"));
        }

        List<String> chunks = documentDBService.chunkText(text);
        List<Integer> ids = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            float[] embedding = ollamaService.embed(chunks.get(i));
            if (embedding.length == 0) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", "Ollama unavailable. Ensure it is running."));
            }
            String chunkTitle = chunks.size() > 1 ? title + " [" + (i + 1) + "/" + chunks.size() + "]" : title;
            int id = documentDBService.insert(chunkTitle, chunks.get(i), embedding);
            ids.add(id);
        }

        return ResponseEntity.ok(Map.of("ids", ids, "chunks", chunks.size(), "dims", documentDBService.getDimensions()));
    }

    // FILE UPLOAD INSERT
    @PostMapping("/doc/upload")
    public ResponseEntity<Map<String, Object>> uploadDocument(@RequestParam("file") MultipartFile file, @RequestParam(value = "title", required = false) String title) {
        // Validate file
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No file uploaded"));
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid filename")
            );
        }

        // Check if file type is supported
        if (!fileParserService.isSupportedFile(filename)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Unsupported file type. Supported: " +
                                    String.join(", ", fileParserService.getSupportedExtensions())));
        }

        // Check file size (10MB limit set in application.properties)
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (file.getSize() > maxSize) {
            return ResponseEntity.badRequest().body(Map.of("error", "File too large. Maximum size: 10MB")
            );
        }

        try {
            // Parse file to extract text
            String extractedText = fileParserService.parseFile(file);

            if (extractedText == null || extractedText.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No text could be extracted from file")
                );
            }

            // Use filename as title if not provided
            String docTitle = (title != null && !title.isEmpty()) ? title : filename;

            // Process and insert
            return processAndInsertDocument(docTitle, extractedText);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to process file: " + e.getMessage())
            );
        }
    }

    private ResponseEntity<Map<String, Object>> processAndInsertDocument(String title, String text) {
        // Chunk the text first
        List<String> chunks = documentDBService.chunkText(text);

        System.out.println("📦 Created " + chunks.size() + " chunks");

        List<Integer> ids = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);

            // Validate chunk size
            if (!documentDBService.isChunkSizeValid(chunk)) {
                System.err.println("⚠️  Chunk " + (i+1) + " is too large (" +
                        documentDBService.estimateTokens(chunk) + " tokens). Skipping...");
                warnings.add("Chunk " + (i+1) + " too large (skipped)");
                continue;
            }

            System.out.println("📤 Embedding chunk " + (i+1) + "/" + chunks.size() +
                    " (" + chunk.split("\\s+").length + " words, ~" +
                    documentDBService.estimateTokens(chunk) + " tokens)");

            float[] embedding = ollamaService.embed(chunk);

            if (embedding.length == 0) {
                System.err.println("❌ Failed to embed chunk " + (i+1));
                warnings.add("Failed to embed chunk " + (i+1));
                continue;
            }

            String chunkTitle = chunks.size() > 1
                    ? title + " [" + (i + 1) + "/" + chunks.size() + "]"
                    : title;

            ids.add(documentDBService.insert(chunkTitle, chunk, embedding));
            System.out.println("✅ Inserted chunk " + (i+1) + " with ID " + ids.get(ids.size()-1));
        }

        if (ids.isEmpty()) {
            return ResponseEntity.status(503).body(
                    Map.of(
                            "error", "Failed to embed any chunks. " +
                                    (!warnings.isEmpty() ? String.join(", ", warnings) : "Ollama may be unavailable.")
                    )
            );
        }

        Map<String, Object> response = new HashMap<>();
        response.put("ids", ids);
        response.put("chunks", chunks.size());
        response.put("successfulChunks", ids.size());
        response.put("dims", documentDBService.getDimensions());
        response.put("wordCount", text.split("\\s+").length);

        if (!warnings.isEmpty()) {
            response.put("warnings", warnings);
        }

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/doc/delete/{id}")
    public ResponseEntity<Map<String, Boolean>> deleteDocument(@PathVariable int id) {
        boolean success = documentDBService.remove(id);
        return ResponseEntity.ok(Map.of("ok", success));
    }

    @GetMapping("/doc/list")
    public ResponseEntity<List<DocumentResponse>> listDocuments() {
        List<DocumentResponse> response = documentDBService.getAllDocs()
                .stream()
                .map(doc -> new DocumentResponse(
                                doc.getId(),
                                doc.getTitle(),
                                doc.getPreview(),
                                doc.getWordCount()
                        )
                )
                .toList();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/doc/search")
    public ResponseEntity<?> searchDocuments(@RequestBody AskQuestionRequest request) {
        String question = request.getQuestion();
        int k = request.getK() != null ? request.getK() : 3;
        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Question is required"));
        }
        float[] queryEmb = ollamaService.embed(question);

        if (queryEmb.length == 0) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", "Ollama unavailable"));
        }
        List<Map.Entry<Float, DocItem>> results = documentDBService.search(queryEmb, k, 0.7f);

        List<SearchContextResponse> contexts = results.stream()
                .map(entry ->
                        new SearchContextResponse(
                                entry.getValue().getId(),
                                entry.getValue().getTitle(),
                                null,
                                entry.getKey()
                        )
                )
                .toList();

        return ResponseEntity.ok(Map.of("contexts", contexts));
    }

    @PostMapping("/doc/ask")
    public ResponseEntity<?> askQuestion(@RequestBody AskQuestionRequest request) {
        String question = request.getQuestion();
        int k = request.getK() != null ? request.getK() : 3;
        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Question is required"));
        }
        float[] queryEmb = ollamaService.embed(question);

        if (queryEmb.length == 0) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", "Ollama unavailable"));
        }

        List<Map.Entry<Float, DocItem>> results = documentDBService.search(queryEmb, k, 0.7f);

        if (results.isEmpty()) {
            AskResponse emptyResponse = new AskResponse("No relevant documents found. Please insert documents first.",
                    ollamaService.getGenerateModel(), Collections.emptyList(), documentDBService.size());
            return ResponseEntity.ok(emptyResponse);
        }
        StringBuilder contextBuilder = new StringBuilder();

        for (int i = 0; i < results.size(); i++) {
            DocItem doc = results.get(i).getValue();
            contextBuilder.append("[")
                    .append(i + 1)
                    .append("] ")
                    .append(doc.getTitle())
                    .append(":\n")
                    .append(doc.getText())
                    .append("\n\n");
        }
        String prompt = "You are a helpful assistant. " + "Answer naturally.\n\n" + "Context:\n" + contextBuilder + "Question: " + question + "\n\nAnswer:";
        String answer = ollamaService.generate(prompt);

        List<SearchContextResponse> contexts = results.stream()
                .map(entry ->
                        new SearchContextResponse(
                                entry.getValue().getId(),
                                entry.getValue().getTitle(),
                                entry.getValue().getText(),
                                entry.getKey()
                        )
                )
                .toList();

        AskResponse response = new AskResponse(answer, ollamaService.getGenerateModel(), contexts, documentDBService.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<?> status() {
        boolean available = ollamaService.isAvailable();
        return ResponseEntity.ok(Map.of(
                        "ollamaAvailable",
                        available,
                        "embedModel",
                        ollamaService.getEmbedModel(),
                        "genModel",
                        ollamaService.getGenerateModel(),
                        "docCount",
                        documentDBService.size(),
                        "docDims",
                        documentDBService.getDimensions()
                )
        );
    }
}