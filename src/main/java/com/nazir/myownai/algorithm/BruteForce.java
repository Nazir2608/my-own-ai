package com.nazir.myownai.algorithm;
import com.nazir.myownai.model.VectorItem;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class BruteForce {
    private final List<VectorItem> items = new CopyOnWriteArrayList<>();

    public void insert(VectorItem item) {
        items.add(item);
    }

    public void remove(int id) {
        items.removeIf(item -> item.getId() == id);
    }

    public List<Map.Entry<Float, Integer>> knn(float[] query, int k, DistanceMetric.DistanceFunction distFn) {
        List<Map.Entry<Float, Integer>> results = new ArrayList<>();

        for (VectorItem item : items) {
            float distance = distFn.calculate(query, item.getEmbedding());
            results.add(Map.entry(distance, item.getId()));
        }

        results.sort(Map.Entry.comparingByKey());

        return results.subList(0, Math.min(k, results.size()));
    }

    public int size() {
        return items.size();
    }
}
