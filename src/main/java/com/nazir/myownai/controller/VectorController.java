package com.nazir.myownai.controller;

import com.nazir.myownai.model.SearchResult;
import com.nazir.myownai.model.VectorItem;
import com.nazir.myownai.service.VectorDBService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class VectorController {

    @Autowired
    private VectorDBService vectorDBService;

    @GetMapping("/search")
    public ResponseEntity<SearchResult> search(@RequestParam String v, @RequestParam(defaultValue = "5") int k, @RequestParam(defaultValue = "cosine") String metric, @RequestParam(defaultValue = "hnsw") String algo) {
        float[] query = parseFloatArray(v);
        if (query.length != vectorDBService.getDimensions()) {
            return ResponseEntity.badRequest().build();
        }
        SearchResult result = vectorDBService.search(query, k, metric, algo);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/insert")
    public ResponseEntity<Map<String, Integer>> insert(@RequestBody Map<String, Object> body) {
        String metadata = (String) body.get("metadata");
        String category = (String) body.get("category");
        List<Double> embeddingList = (List<Double>) body.get("embedding");

        float[] embedding = new float[embeddingList.size()];
        for (int i = 0; i < embeddingList.size(); i++) {
            embedding[i] = embeddingList.get(i).floatValue();
        }

        int id = vectorDBService.insert(metadata, category, embedding);
        return ResponseEntity.ok(Map.of("id", id));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, Boolean>> delete(@PathVariable int id) {
        boolean success = vectorDBService.remove(id);
        return ResponseEntity.ok(Map.of("ok", success));
    }

    @GetMapping("/items")
    public ResponseEntity<List<VectorItem>> getAllItems() {
        return ResponseEntity.ok(vectorDBService.getAllItems());
    }

    @GetMapping("/benchmark")
    public ResponseEntity<Map<String, Object>> benchmark(@RequestParam String v, @RequestParam(defaultValue = "5") int k, @RequestParam(defaultValue = "cosine") String metric) {
        float[] query = parseFloatArray(v);
        Map<String, Object> result = vectorDBService.benchmark(query, k, metric);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/hnsw-info")
    public ResponseEntity<Map<String, Object>> hnswInfo() {
        return ResponseEntity.ok(vectorDBService.getHnswInfo());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(Map.of(
                "count", vectorDBService.size(),
                "dims", vectorDBService.getDimensions(),
                "algorithms", List.of("bruteforce", "kdtree", "hnsw"),
                "metrics", List.of("euclidean", "cosine", "manhattan")
        ));
    }

    private float[] parseFloatArray(String s) {
        String[] parts = s.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }
}
