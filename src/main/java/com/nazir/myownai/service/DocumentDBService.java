package com.nazir.myownai.service;

import com.nazir.myownai.algorithm.DistanceMetric;
import com.nazir.myownai.algorithm.HNSW;
import com.nazir.myownai.entity.DocumentEntity;
import com.nazir.myownai.model.DocItem;
import com.nazir.myownai.model.VectorItem;
import com.nazir.myownai.repository.DocumentRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DocumentDBService {

    @Value("${document.chunk.words:150}")
    private int chunkWords;

    @Value("${document.chunk.overlap:20}")
    private int overlapWords;

    @Value("${document.max-words-per-chunk:150}")
    private int maxWordsPerChunk;

    @Value("${document.max-tokens-per-chunk:512}")
    private int maxTokensPerChunk;

    @Value("${vectordb.hnsw.m:16}")
    private int hnswM;

    @Value("${vectordb.hnsw.ef-construction:200}")
    private int hnswEfConstruction;

    @Autowired
    private DocumentRepository documentRepository;

    // In-memory HNSW index (rebuilt from DB on startup)
    private HNSW hnsw;
    private int dimensions = 0;

    @PostConstruct
    public void init() {
        hnsw = new HNSW(hnswM, hnswEfConstruction);
        rebuildIndex();
        System.out.println("✅ DocumentDBService initialized with " + documentRepository.count() + " documents from database");
    }

    /**
     * Rebuild in-memory HNSW index from database
     */
    @Transactional(readOnly = true)
    public void rebuildIndex() {
        hnsw = new HNSW(hnswM, hnswEfConstruction);

        List<DocumentEntity> all = documentRepository.findAll();
        for (DocumentEntity entity : all) {
            if (dimensions == 0) {
                dimensions = entity.getEmbedding().length;
            }
            VectorItem item = new VectorItem(
                    entity.getId(),
                    entity.getTitle(),
                    "doc",
                    entity.getEmbedding()
            );
            hnsw.insert(item, DistanceMetric::cosine);
        }

        System.out.println("🔄 Rebuilt document index with " + all.size() + " chunks");
    }

    @Transactional
    public int insert(String title, String text, float[] embedding) {
        if (dimensions == 0) {
            dimensions = embedding.length;
        }

        // Save to PostgreSQL
        DocumentEntity entity = new DocumentEntity(title, text, embedding);
        DocumentEntity saved = documentRepository.save(entity);

        // Add to in-memory HNSW index
        VectorItem item = new VectorItem(saved.getId(), title, "doc", embedding);
        hnsw.insert(item, DistanceMetric::cosine);

        return saved.getId();
    }

    public List<Map.Entry<Float, DocItem>> search(float[] query, int k, float maxDistance) {
        if (documentRepository.count() == 0) {
            return Collections.emptyList();
        }

        List<Map.Entry<Float, Integer>> results = hnsw.knn(query, k, 50, DistanceMetric::cosine);

        // Fetch full details from database
        List<Map.Entry<Float, DocItem>> output = new ArrayList<>();
        for (Map.Entry<Float, Integer> entry : results) {
            documentRepository.findById(entry.getValue()).ifPresent(entity -> {
                if (entry.getKey() <= maxDistance) {
                    DocItem docItem = new DocItem(
                            entity.getId(),
                            entity.getTitle(),
                            entity.getText(),
                            entity.getEmbedding()
                    );
                    output.add(Map.entry(entry.getKey(), docItem));
                }
            });
        }

        return output;
    }

    @Transactional
    public boolean remove(int id) {
        if (!documentRepository.existsById(id)) {
            return false;
        }

        documentRepository.deleteById(id);
        rebuildIndex();

        return true;
    }

    @Transactional(readOnly = true)
    public List<DocItem> getAllDocs() {
        return documentRepository.findAll().stream()
                .map(entity -> new DocItem(
                        entity.getId(),
                        entity.getTitle(),
                        entity.getText(),
                        entity.getEmbedding()
                ))
                .collect(Collectors.toList());
    }

    public long size() {
        return documentRepository.count();
    }

    public int getDimensions() {
        return dimensions;
    }

    // Chunking methods (keep as is)
    public List<String> chunkText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String[] words = text.split("\\s+");
        int totalWords = words.length;

        int effectiveChunkSize = Math.min(chunkWords, maxWordsPerChunk);
        int effectiveOverlap = Math.min(overlapWords, effectiveChunkSize / 3);

        List<String> chunks = new ArrayList<>();

        if (totalWords <= effectiveChunkSize) {
            chunks.add(text.trim());
            return chunks;
        }

        int step = effectiveChunkSize - effectiveOverlap;

        for (int i = 0; i < totalWords; i += step) {
            int end = Math.min(i + effectiveChunkSize, totalWords);

            StringBuilder chunkBuilder = new StringBuilder();
            for (int j = i; j < end; j++) {
                if (j > i) chunkBuilder.append(" ");
                chunkBuilder.append(words[j]);
            }

            String chunk = chunkBuilder.toString().trim();

            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            if (end == totalWords) {
                break;
            }
        }

        return chunks;
    }

    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int wordCount = text.split("\\s+").length;
        return (int) Math.ceil(wordCount * 1.3);
    }

    public boolean isChunkSizeValid(String chunk) {
        int estimatedTokens = estimateTokens(chunk);
        return estimatedTokens <= maxTokensPerChunk;
    }
}