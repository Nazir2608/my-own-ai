package com.nazir.myownai.service;

import com.nazir.myownai.algorithm.DistanceMetric;
import com.nazir.myownai.algorithm.HNSW;
import com.nazir.myownai.model.DocItem;
import com.nazir.myownai.model.VectorItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class DocumentDBService {

    @Value("${document.chunk.words:150}")
    private int chunkWords;

    @Value("${document.chunk.overlap:20}")
    private int overlapWords;

    @Value("${document.max-words-per-chunk:150}")
    private int maxWordsPerChunk;  // ADD THIS LINE

    @Value("${document.max-tokens-per-chunk:512}")
    private int maxTokensPerChunk;  // ADD THIS LINE

    @Value("${vectordb.hnsw.m:16}")
    private int hnswM;

    @Value("${vectordb.hnsw.ef-construction:200}")
    private int hnswEfConstruction;

    private final Map<Integer, DocItem> store = new ConcurrentHashMap<>();
    private final HNSW hnsw = new HNSW(16, 200);
    private int nextId = 1;
    private int dimensions = 0;

    public int insert(String title, String text, float[] embedding) {
        if (dimensions == 0) {
            dimensions = embedding.length;
        }

        int id = nextId++;
        DocItem item = new DocItem(id, title, text, embedding);
        store.put(id, item);

        VectorItem vectorItem = new VectorItem(id, title, "doc", embedding);
        hnsw.insert(vectorItem, DistanceMetric::cosine);

        return id;
    }

    public List<Map.Entry<Float, DocItem>> search(float[] query, int k, float maxDistance) {
        if (store.isEmpty()) return Collections.emptyList();

        List<Map.Entry<Float, Integer>> results = hnsw.knn(query, k, 50, DistanceMetric::cosine);

        return results.stream()
                .map(entry -> {
                    DocItem item = store.get(entry.getValue());
                    return item != null ? Map.entry(entry.getKey(), item) : null;
                })
                .filter(Objects::nonNull)
                .filter(entry -> entry.getKey() <= maxDistance)
                .collect(Collectors.toList());
    }

    public boolean remove(int id) {
        DocItem removed = store.remove(id);
        if (removed == null) return false;
        hnsw.remove(id);
        return true;
    }

    public List<DocItem> getAllDocs() {
        return new ArrayList<>(store.values());
    }

    public int size() {
        return store.size();
    }

    public int getDimensions() {
        return dimensions;
    }

    public List<String> chunkText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String[] words = text.split("\\s+");
        int totalWords = words.length;

        // Use configured chunk size, but never exceed max
        int effectiveChunkSize = Math.min(chunkWords, maxWordsPerChunk);
        int effectiveOverlap = Math.min(overlapWords, effectiveChunkSize / 3);

        List<String> chunks = new ArrayList<>();

        if (totalWords <= effectiveChunkSize) {
            // Small document - return as single chunk
            chunks.add(text.trim());
            return chunks;
        }

        // Large document - chunk with overlap
        int step = effectiveChunkSize - effectiveOverlap;

        for (int i = 0; i < totalWords; i += step) {
            int end = Math.min(i + effectiveChunkSize, totalWords);

            // Build chunk from word array
            StringBuilder chunkBuilder = new StringBuilder();
            for (int j = i; j < end; j++) {
                if (j > i) chunkBuilder.append(" ");
                chunkBuilder.append(words[j]);
            }

            String chunk = chunkBuilder.toString().trim();

            // Only add non-empty chunks
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            // Break if we've reached the end
            if (end == totalWords) {
                break;
            }
        }

        return chunks;
    }

    /**
     * Estimate token count (rough approximation)
     * Average: 1 word ≈ 1.3 tokens
     */
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int wordCount = text.split("\\s+").length;
        return (int) Math.ceil(wordCount * 1.3);
    }

    /**
     * Validate chunk size before embedding
     */
    public boolean isChunkSizeValid(String chunk) {
        int estimatedTokens = estimateTokens(chunk);
        // nomic-embed-text max: 8192 tokens
        // We use 512 as safe limit per chunk
        return estimatedTokens <= maxTokensPerChunk;
    }
}
