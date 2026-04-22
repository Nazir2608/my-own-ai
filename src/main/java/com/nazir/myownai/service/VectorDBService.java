package com.nazir.myownai.service;

import com.nazir.myownai.algorithm.DistanceMetric;
import com.nazir.myownai.algorithm.HNSW;
import com.nazir.myownai.algorithm.KDTree;
import com.nazir.myownai.algorithm.BruteForce;
import com.nazir.myownai.entity.VectorEntity;
import com.nazir.myownai.model.SearchResult;
import com.nazir.myownai.model.VectorItem;
import com.nazir.myownai.repository.VectorRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class VectorDBService {

    @Value("${vectordb.demo.dimensions:16}")
    private int dimensions;

    @Value("${vectordb.hnsw.m:16}")
    private int hnswM;

    @Value("${vectordb.hnsw.ef-construction:200}")
    private int hnswEfConstruction;

    @Autowired
    private VectorRepository vectorRepository;

    // In-memory indexes for demo (rebuilt from DB on startup)
    private final BruteForce bruteForce = new BruteForce();
    private KDTree kdTree;
    private HNSW hnsw;

    @PostConstruct
    public void init() {
        kdTree = new KDTree(dimensions);
        hnsw = new HNSW(hnswM, hnswEfConstruction);

        // Load existing vectors from database into memory indexes
        rebuildIndexes();

        System.out.println("✅ VectorDBService initialized with " + vectorRepository.count() + " vectors from database");
    }

    /**
     * Rebuild in-memory indexes from database
     */
    @Transactional(readOnly = true)
    public void rebuildIndexes() {
        bruteForce.items.clear();
        kdTree = new KDTree(dimensions);
        hnsw = new HNSW(hnswM, hnswEfConstruction);

        List<VectorEntity> all = vectorRepository.findAll();
        for (VectorEntity entity : all) {
            VectorItem item = new VectorItem(
                    entity.getId(),
                    entity.getMetadata(),
                    entity.getCategory(),
                    entity.getEmbedding()
            );
            bruteForce.insert(item);
            kdTree.insert(item);
            hnsw.insert(item, DistanceMetric::cosine);
        }

        System.out.println("🔄 Rebuilt indexes with " + all.size() + " vectors");
    }

    @Transactional
    public int insert(String metadata, String category, float[] embedding) {
        // Save to PostgreSQL
        VectorEntity entity = new VectorEntity(metadata, category, embedding);
        VectorEntity saved = vectorRepository.save(entity);

        // Add to in-memory indexes
        VectorItem item = new VectorItem(saved.getId(), metadata, category, embedding);
        bruteForce.insert(item);
        kdTree.insert(item);
        hnsw.insert(item, DistanceMetric::cosine);

        return saved.getId();
    }

    @Transactional
    public boolean remove(int id) {
        if (!vectorRepository.existsById(id)) {
            return false;
        }

        // Delete from PostgreSQL
        vectorRepository.deleteById(id);

        // Rebuild in-memory indexes
        rebuildIndexes();

        return true;
    }

    public SearchResult search(float[] query, int k, String metric, String algorithm) {
        DistanceMetric.DistanceFunction distFn = DistanceMetric.getFunction(metric);

        long startTime = System.nanoTime();
        List<Map.Entry<Float, Integer>> rawResults;

        switch (algorithm.toLowerCase()) {
            case "bruteforce" -> rawResults = bruteForce.knn(query, k, distFn);
            case "kdtree" -> rawResults = kdTree.knn(query, k, distFn);
            default -> rawResults = hnsw.knn(query, k, 50, distFn);
        }

        long latencyUs = (System.nanoTime() - startTime) / 1000;

        // Fetch full details from database
        List<SearchResult.Hit> hits = new ArrayList<>();
        for (Map.Entry<Float, Integer> entry : rawResults) {
            vectorRepository.findById(entry.getValue()).ifPresent(entity -> {
                hits.add(new SearchResult.Hit(
                        entity.getId(),
                        entity.getMetadata(),
                        entity.getCategory(),
                        entity.getEmbedding(),
                        entry.getKey()
                ));
            });
        }

        return new SearchResult(hits, latencyUs, algorithm, metric);
    }

    public Map<String, Object> benchmark(float[] query, int k, String metric) {
        DistanceMetric.DistanceFunction distFn = DistanceMetric.getFunction(metric);

        long bfStart = System.nanoTime();
        bruteForce.knn(query, k, distFn);
        long bfUs = (System.nanoTime() - bfStart) / 1000;

        long kdStart = System.nanoTime();
        kdTree.knn(query, k, distFn);
        long kdUs = (System.nanoTime() - kdStart) / 1000;

        long hnswStart = System.nanoTime();
        hnsw.knn(query, k, 50, distFn);
        long hnswUs = (System.nanoTime() - hnswStart) / 1000;

        return Map.of(
                "bruteforceUs", bfUs,
                "kdtreeUs", kdUs,
                "hnswUs", hnswUs,
                "itemCount", vectorRepository.count()
        );
    }

    @Transactional(readOnly = true)
    public List<VectorItem> getAllItems() {
        return vectorRepository.findAll().stream()
                .map(entity -> new VectorItem(
                        entity.getId(),
                        entity.getMetadata(),
                        entity.getCategory(),
                        entity.getEmbedding()
                ))
                .collect(Collectors.toList());
    }

    public Map<String, Object> getHnswInfo() {
        return hnsw.getInfo();
    }

    public long size() {
        return vectorRepository.count();
    }

    public int getDimensions() {
        return dimensions;
    }
}