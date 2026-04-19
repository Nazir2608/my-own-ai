package com.nazir.myownai.algorithm;
import com.nazir.myownai.model.VectorItem;
import java.util.*;

public class KDTree {
    private Node root;
    private final int dimensions;

    private static class Node {
        VectorItem item;
        Node left, right;

        Node(VectorItem item) {
            this.item = item;
        }
    }

    public KDTree(int dimensions) {
        this.dimensions = dimensions;
    }

    public void insert(VectorItem item) {
        root = insert(root, item, 0);
    }

    private Node insert(Node node, VectorItem item, int depth) {
        if (node == null) return new Node(item);

        int axis = depth % dimensions;
        if (item.getEmbedding()[axis] < node.item.getEmbedding()[axis]) {
            node.left = insert(node.left, item, depth + 1);
        } else {
            node.right = insert(node.right, item, depth + 1);
        }
        return node;
    }

    public List<Map.Entry<Float, Integer>> knn(float[] query, int k, DistanceMetric.DistanceFunction distFn) {
        PriorityQueue<Map.Entry<Float, Integer>> heap = new PriorityQueue<>((a, b) -> Float.compare(b.getKey(), a.getKey()));

        knnSearch(root, query, k, 0, distFn, heap);

        List<Map.Entry<Float, Integer>> results = new ArrayList<>(heap);
        results.sort(Map.Entry.comparingByKey());
        return results;
    }

    private void knnSearch(Node node, float[] query, int k, int depth,
                           DistanceMetric.DistanceFunction distFn,
                           PriorityQueue<Map.Entry<Float, Integer>> heap) {
        if (node == null) return;

        float distance = distFn.calculate(query, node.item.getEmbedding());

        if (heap.size() < k || distance < heap.peek().getKey()) {
            heap.offer(Map.entry(distance, node.item.getId()));
            if (heap.size() > k) heap.poll();
        }

        int axis = depth % dimensions;
        float diff = query[axis] - node.item.getEmbedding()[axis];

        Node closer = diff < 0 ? node.left : node.right;
        Node farther = diff < 0 ? node.right : node.left;

        knnSearch(closer, query, k, depth + 1, distFn, heap);

        if (heap.size() < k || Math.abs(diff) < heap.peek().getKey()) {
            knnSearch(farther, query, k, depth + 1, distFn, heap);
        }
    }

    public void rebuild(List<VectorItem> items) {
        root = null;
        for (VectorItem item : items) {
            insert(item);
        }
    }
}
