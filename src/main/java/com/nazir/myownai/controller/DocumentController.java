package com.nazir.myownai.controller;

import com.nazir.myownai.model.DocItem;
import com.nazir.myownai.service.DocumentDBService;
import com.nazir.myownai.service.OllamaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class DocumentController {

    @Autowired
    private DocumentDBService documentDBService;

    @Autowired
    private OllamaService ollamaService;

    @PostMapping("/doc/insert")
    public ResponseEntity<Map<String, Object>> insertDocument(@RequestBody Map<String, String> body) {
        String title = body.get("title");
        String text = body.get("text");

        if (title == null || text == null || title.isEmpty() || text.isEmpty()) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "Title and text are required")
            );
        }

        List<String> chunks = documentDBService.chunkText(text);
        List<Integer> ids = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            float[] embedding = ollamaService.embed(chunks.get(i));
            
            if (embedding.length == 0) {
                return ResponseEntity.status(503).body(
                    Map.of("error", "Ollama unavailable. Make sure it's running and nomic-embed-text is installed.")
                );
            }

            String chunkTitle = chunks.size() > 1 
                ? title + " [" + (i + 1) + "/" + chunks.size() + "]"
                : title;
            
            ids.add(documentDBService.insert(chunkTitle, chunks.get(i), embedding));
        }

        return ResponseEntity.ok(Map.of(
            "ids", ids,
            "chunks", chunks.size(),
            "dims", documentDBService.getDimensions()
        ));
    }

    @DeleteMapping("/doc/delete/{id}")
    public ResponseEntity<Map<String, Boolean>> deleteDocument(@PathVariable int id) {
        boolean success = documentDBService.remove(id);
        return ResponseEntity.ok(Map.of("ok", success));
    }

    @GetMapping("/doc/list")
    public ResponseEntity<List<Map<String, Object>>> listDocuments() {
        List<DocItem> docs = documentDBService.getAllDocs();
        
        List<Map<String, Object>> response = docs.stream()
                .map(doc -> Map.<String, Object>of(
                "id", doc.getId(),
                "title", doc.getTitle(),
                "preview", doc.getPreview(),
                "words", doc.getWordCount()
            ))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/doc/search")
    public ResponseEntity<Map<String, Object>> searchDocuments(@RequestBody Map<String, Object> body) {
        String question = (String) body.get("question");
        int k = body.containsKey("k") ? ((Number) body.get("k")).intValue() : 3;

        if (question == null || question.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Question is required"));
        }

        float[] queryEmb = ollamaService.embed(question);
        if (queryEmb.length == 0) {
            return ResponseEntity.status(503).body(Map.of("error", "Ollama unavailable"));
        }

        List<Map.Entry<Float, DocItem>> results = documentDBService.search(queryEmb, k, 0.7f);

        List<Map<String, Object>> contexts = results.stream()
                .map(entry -> Map.<String, Object>of(
                "id", entry.getValue().getId(),
                "title", entry.getValue().getTitle(),
                "distance", entry.getKey()
            ))
            .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("contexts", contexts));
    }

    @PostMapping("/doc/ask")
    public ResponseEntity<Map<String, Object>> askQuestion(@RequestBody Map<String, Object> body) {
        String question = (String) body.get("question");
        int k = body.containsKey("k") ? ((Number) body.get("k")).intValue() : 3;

        if (question == null || question.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Question is required"));
        }

        // Step 1: Embed the question
        float[] queryEmb = ollamaService.embed(question);
        if (queryEmb.length == 0) {
            return ResponseEntity.status(503).body(
                Map.of("error", "Ollama unavailable. Ensure it's running and models are loaded.")
            );
        }

        // Step 2: Retrieve relevant chunks
        List<Map.Entry<Float, DocItem>> results = documentDBService.search(queryEmb, k, 0.7f);

        if (results.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "answer", "No relevant documents found. Please insert some documents first.",
                "model", ollamaService.getGenerateModel(),
                "contexts", Collections.emptyList(),
                "docCount", documentDBService.size()
            ));
        }

        // Step 3: Build prompt with context
        StringBuilder contextBuilder = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            DocItem doc = results.get(i).getValue();
            contextBuilder.append("[").append(i + 1).append("] ")
                          .append(doc.getTitle()).append(":\n")
                          .append(doc.getText()).append("\n\n");
        }

        String prompt = "You are a helpful assistant. Answer the user's question directly. " +
                       "Use the provided context if it contains relevant information. " +
                       "If it doesn't, just use your own general knowledge. " +
                       "IMPORTANT: Do NOT mention the 'context', 'provided text', or say things like 'the context doesn't mention'. " +
                       "Just answer the question naturally.\n\n" +
                       "Context:\n" + contextBuilder.toString() +
                       "Question: " + question + "\n\n" +
                       "Answer:";

        // Step 4: Generate answer
        String answer = ollamaService.generate(prompt);

        // Step 5: Return everything
        List<Map<String, Object>> contexts = results.stream()
                .map(entry -> Map.<String, Object>of(
                "id", entry.getValue().getId(),
                "title", entry.getValue().getTitle(),
                "text", entry.getValue().getText(),
                "distance", entry.getKey()
            ))
            .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
            "answer", answer,
            "model", ollamaService.getGenerateModel(),
            "contexts", contexts,
            "docCount", documentDBService.size()
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        boolean available = ollamaService.isAvailable();
        
        return ResponseEntity.ok(Map.of(
            "ollamaAvailable", available,
            "embedModel", ollamaService.getEmbedModel(),
            "genModel", ollamaService.getGenerateModel(),
            "docCount", documentDBService.size(),
            "docDims", documentDBService.getDimensions()
        ));
    }
}
