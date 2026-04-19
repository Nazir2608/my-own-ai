package com.nazir.myownai.controller;

import com.nazir.myownai.dto.AskQuestionRequest;
import com.nazir.myownai.dto.AskResponse;
import com.nazir.myownai.dto.DocumentResponse;
import com.nazir.myownai.dto.InsertDocumentRequest;
import com.nazir.myownai.dto.SearchContextResponse;
import com.nazir.myownai.model.DocItem;
import com.nazir.myownai.service.DocumentDBService;
import com.nazir.myownai.service.OllamaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class DocumentController {

    private final DocumentDBService documentDBService;
    private final OllamaService ollamaService;

    public DocumentController(DocumentDBService documentDBService, OllamaService ollamaService) {
        this.documentDBService = documentDBService;
        this.ollamaService = ollamaService;
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