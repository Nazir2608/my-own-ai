package com.myownai.service;

import com.myownai.algorithm.DistanceMetric;
import com.myownai.algorithm.HNSW;
import com.myownai.model.DocItem;
import com.myownai.model.VectorItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class DocumentDBService {

    @Value("${document.chunk.words:250}")
    private int chunkWords;

    @Value("${document.chunk.overlap:30}")
    private int overlapWords;

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
        String[] words = text.split("\\s+");
        
        if (words.length <= chunkWords) {
            return Collections.singletonList(text);
        }

        List<String> chunks = new ArrayList<>();
        int step = chunkWords - overlapWords;
        
        for (int i = 0; i < words.length; i += step) {
            int end = Math.min(i + chunkWords, words.length);
            String chunk = String.join(" ", Arrays.copyOfRange(words, i, end));
            chunks.add(chunk);
            if (end == words.length) break;
        }
        
        return chunks;
    }
}
