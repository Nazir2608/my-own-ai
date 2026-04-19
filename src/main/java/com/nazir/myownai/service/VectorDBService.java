package com.nazir.myownai.service;
import com.nazir.myownai.algorithm.BruteForce;
import com.nazir.myownai.algorithm.DistanceMetric;
import com.nazir.myownai.algorithm.HNSW;
import com.nazir.myownai.algorithm.KDTree;
import com.nazir.myownai.model.SearchResult;
import com.nazir.myownai.model.VectorItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VectorDBService {

    @Value("${vectordb.demo.dimensions:16}")
    private int dimensions;

    @Value("${vectordb.hnsw.m:16}")
    private int hnswM;

    @Value("${vectordb.hnsw.ef-construction:200}")
    private int hnswEfConstruction;

    private final Map<Integer, VectorItem> store = new ConcurrentHashMap<>();
    private final BruteForce bruteForce = new BruteForce();
    private KDTree kdTree;
    private HNSW hnsw;
    private int nextId = 1;

    @PostConstruct
    public void init() {
        kdTree = new KDTree(dimensions);
        hnsw = new HNSW(hnswM, hnswEfConstruction);
        loadDemoData();
    }

    public int insert(String metadata, String category, float[] embedding) {
        int id = nextId++;
        VectorItem item = new VectorItem(id, metadata, category, embedding);
        
        store.put(id, item);
        bruteForce.insert(item);
        kdTree.insert(item);
        hnsw.insert(item, DistanceMetric::cosine);
        
        return id;
    }

    public boolean remove(int id) {
        VectorItem removed = store.remove(id);
        if (removed == null) return false;

        bruteForce.remove(id);
        hnsw.remove(id);
        
        // Rebuild KD-Tree
        kdTree.rebuild(new ArrayList<>(store.values()));
        
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

        List<SearchResult.Hit> hits = new ArrayList<>();
        for (Map.Entry<Float, Integer> entry : rawResults) {
            VectorItem item = store.get(entry.getValue());
            if (item != null) {
                hits.add(new SearchResult.Hit(
                    item.getId(),
                    item.getMetadata(),
                    item.getCategory(),
                    item.getEmbedding(),
                    entry.getKey()
                ));
            }
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
            "itemCount", store.size()
        );
    }

    public List<VectorItem> getAllItems() {
        return new ArrayList<>(store.values());
    }

    public Map<String, Object> getHnswInfo() {
        return hnsw.getInfo();
    }

    public int size() {
        return store.size();
    }

    public int getDimensions() {
        return dimensions;
    }

    private void loadDemoData() {
        // CS / Algorithms
        insert("Linked List: nodes connected by pointers", "cs",
            new float[]{0.90f,0.85f,0.72f,0.68f,0.12f,0.08f,0.15f,0.10f,0.05f,0.08f,0.06f,0.09f,0.07f,0.11f,0.08f,0.06f});
        insert("Binary Search Tree: O(log n) search and insert", "cs",
            new float[]{0.88f,0.82f,0.78f,0.74f,0.15f,0.10f,0.08f,0.12f,0.06f,0.07f,0.08f,0.05f,0.09f,0.06f,0.07f,0.10f});
        insert("Dynamic Programming: memoization overlapping subproblems", "cs",
            new float[]{0.82f,0.76f,0.88f,0.80f,0.20f,0.18f,0.12f,0.09f,0.07f,0.06f,0.08f,0.07f,0.08f,0.09f,0.06f,0.07f});
        insert("Graph BFS and DFS: breadth and depth first traversal", "cs",
            new float[]{0.85f,0.80f,0.75f,0.82f,0.18f,0.14f,0.10f,0.08f,0.06f,0.09f,0.07f,0.06f,0.10f,0.08f,0.09f,0.07f});
        insert("Hash Table: O(1) lookup with collision chaining", "cs",
            new float[]{0.87f,0.78f,0.70f,0.76f,0.13f,0.11f,0.09f,0.14f,0.08f,0.07f,0.06f,0.08f,0.07f,0.10f,0.08f,0.09f});

        // Mathematics
        insert("Calculus: derivatives integrals and limits", "math",
            new float[]{0.12f,0.15f,0.18f,0.10f,0.91f,0.86f,0.78f,0.72f,0.08f,0.06f,0.07f,0.09f,0.07f,0.08f,0.06f,0.10f});
        insert("Linear Algebra: matrices eigenvalues eigenvectors", "math",
            new float[]{0.20f,0.18f,0.15f,0.12f,0.88f,0.90f,0.82f,0.76f,0.09f,0.07f,0.08f,0.06f,0.10f,0.07f,0.08f,0.09f});
        insert("Probability: distributions random variables Bayes theorem", "math",
            new float[]{0.15f,0.12f,0.20f,0.18f,0.84f,0.80f,0.88f,0.82f,0.07f,0.08f,0.06f,0.10f,0.09f,0.06f,0.09f,0.08f});
        insert("Number Theory: primes modular arithmetic RSA cryptography", "math",
            new float[]{0.22f,0.16f,0.14f,0.20f,0.80f,0.85f,0.76f,0.90f,0.08f,0.09f,0.07f,0.06f,0.08f,0.10f,0.07f,0.06f});
        insert("Combinatorics: permutations combinations generating functions", "math",
            new float[]{0.18f,0.20f,0.16f,0.14f,0.86f,0.78f,0.84f,0.80f,0.06f,0.07f,0.09f,0.08f,0.06f,0.09f,0.10f,0.07f});

        // Food
        insert("Neapolitan Pizza: wood-fired dough San Marzano tomatoes", "food",
            new float[]{0.08f,0.06f,0.09f,0.07f,0.07f,0.08f,0.06f,0.09f,0.90f,0.86f,0.78f,0.72f,0.08f,0.06f,0.09f,0.07f});
        insert("Sushi: vinegared rice raw fish and nori rolls", "food",
            new float[]{0.06f,0.08f,0.07f,0.09f,0.09f,0.06f,0.08f,0.07f,0.86f,0.90f,0.82f,0.76f,0.07f,0.09f,0.06f,0.08f});
        insert("Ramen: noodle soup with chashu pork and soft-boiled eggs", "food",
            new float[]{0.09f,0.07f,0.06f,0.08f,0.08f,0.09f,0.07f,0.06f,0.82f,0.78f,0.90f,0.84f,0.09f,0.07f,0.08f,0.06f});
        insert("Tacos: corn tortillas with carnitas salsa and cilantro", "food",
            new float[]{0.07f,0.09f,0.08f,0.06f,0.06f,0.07f,0.09f,0.08f,0.78f,0.82f,0.86f,0.90f,0.06f,0.08f,0.07f,0.09f});
        insert("Croissant: laminated pastry with buttery flaky layers", "food",
            new float[]{0.06f,0.07f,0.10f,0.09f,0.10f,0.06f,0.07f,0.10f,0.85f,0.80f,0.76f,0.82f,0.09f,0.07f,0.10f,0.06f});

        // Sports
        insert("Basketball: fast-paced shooting dribbling slam dunks", "sports",
            new float[]{0.09f,0.07f,0.08f,0.10f,0.08f,0.09f,0.07f,0.06f,0.08f,0.07f,0.09f,0.06f,0.91f,0.85f,0.78f,0.72f});
        insert("Football: tackles touchdowns field goals and strategy", "sports",
            new float[]{0.07f,0.09f,0.06f,0.08f,0.09f,0.07f,0.10f,0.08f,0.07f,0.09f,0.08f,0.07f,0.87f,0.89f,0.82f,0.76f});
        insert("Tennis: racket volleys groundstrokes and Wimbledon serves", "sports",
            new float[]{0.08f,0.06f,0.09f,0.07f,0.07f,0.08f,0.06f,0.09f,0.09f,0.06f,0.07f,0.08f,0.83f,0.80f,0.88f,0.82f});
        insert("Chess: openings endgames tactics strategic board game", "sports",
            new float[]{0.25f,0.20f,0.22f,0.18f,0.22f,0.18f,0.20f,0.15f,0.06f,0.08f,0.07f,0.09f,0.80f,0.84f,0.78f,0.90f});
        insert("Swimming: butterfly freestyle backstroke Olympic competition", "sports",
            new float[]{0.06f,0.08f,0.07f,0.09f,0.08f,0.06f,0.09f,0.07f,0.10f,0.08f,0.06f,0.07f,0.85f,0.82f,0.86f,0.80f});
    }
}
