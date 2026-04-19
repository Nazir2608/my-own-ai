package com.nazir.myownai.algorithm;

import com.nazir.myownai.model.VectorItem;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HNSW {
    private final Map<Integer, Node> graph = new ConcurrentHashMap<>();
    private final int M;           // Max connections per layer
    private final int M0;          // Max connections at layer 0
    private final int efConstruction;
    private final double mL;
    private int entryPoint = -1;
    private int topLayer = -1;
    private final Random random = new Random(42);

    private static class Node {
        VectorItem item;
        int maxLayer;
        List<List<Integer>> neighbors;  // neighbors[layer] = list of neighbor IDs

        Node(VectorItem item, int maxLayer) {
            this.item = item;
            this.maxLayer = maxLayer;
            this.neighbors = new ArrayList<>(maxLayer + 1);
            for (int i = 0; i <= maxLayer; i++) {
                this.neighbors.add(new ArrayList<>());
            }
        }
    }

    public HNSW(int M, int efConstruction) {
        this.M = M;
        this.M0 = 2 * M;
        this.efConstruction = efConstruction;
        this.mL = 1.0 / Math.log(M);
    }

    private int randomLevel() {
        return (int) Math.floor(-Math.log(random.nextDouble()) * mL);
    }

    public void insert(VectorItem item, DistanceMetric.DistanceFunction distFn) {
        int id = item.getId();
        int level = randomLevel();
        Node node = new Node(item, level);
        graph.put(id, node);

        if (entryPoint == -1) {
            entryPoint = id;
            topLayer = level;
            return;
        }

        int ep = entryPoint;

        // Search from top layer to insertion layer
        for (int lc = topLayer; lc > level; lc--) {
            List<Map.Entry<Float, Integer>> nearest = searchLayer(item.getEmbedding(), ep, 1, lc, distFn);
            if (!nearest.isEmpty()) ep = nearest.get(0).getValue();
        }

        // Insert at each layer
        for (int lc = Math.min(topLayer, level); lc >= 0; lc--) {
            List<Map.Entry<Float, Integer>> candidates = searchLayer(item.getEmbedding(), ep, efConstruction, lc, distFn);
            int maxM = (lc == 0) ? M0 : M;
            List<Integer> selected = selectNeighbors(candidates, maxM);

            node.neighbors.get(lc).addAll(selected);

            // Add bidirectional links
            for (int neighborId : selected) {
                Node neighbor = graph.get(neighborId);
                if (neighbor != null && lc < neighbor.neighbors.size()) {
                    neighbor.neighbors.get(lc).add(id);

                    // Prune if needed
                    if (neighbor.neighbors.get(lc).size() > maxM) {
                        pruneConnections(neighbor, lc, maxM, distFn);
                    }
                }
            }

            if (!candidates.isEmpty()) ep = candidates.get(0).getValue();
        }

        if (level > topLayer) {
            topLayer = level;
            entryPoint = id;
        }
    }

    private List<Map.Entry<Float, Integer>> searchLayer(float[] query, int entryPoint, int ef, int layer,
                                                        DistanceMetric.DistanceFunction distFn) {
        Set<Integer> visited = new HashSet<>();
        PriorityQueue<Map.Entry<Float, Integer>> candidates =
                new PriorityQueue<>(Map.Entry.comparingByKey());
        PriorityQueue<Map.Entry<Float, Integer>> found =
                new PriorityQueue<>((a, b) -> Float.compare(b.getKey(), a.getKey()));

        Node epNode = graph.get(entryPoint);
        if (epNode == null) return Collections.emptyList();

        float dist = distFn.calculate(query, epNode.item.getEmbedding());
        visited.add(entryPoint);
        candidates.offer(Map.entry(dist, entryPoint));
        found.offer(Map.entry(dist, entryPoint));

        while (!candidates.isEmpty()) {
            Map.Entry<Float, Integer> current = candidates.poll();
            if (found.size() >= ef && current.getKey() > found.peek().getKey()) break;

            Node currentNode = graph.get(current.getValue());
            if (currentNode == null || layer >= currentNode.neighbors.size()) continue;

            for (int neighborId : currentNode.neighbors.get(layer)) {
                if (visited.contains(neighborId)) continue;
                visited.add(neighborId);

                Node neighborNode = graph.get(neighborId);
                if (neighborNode == null) continue;

                float neighborDist = distFn.calculate(query, neighborNode.item.getEmbedding());

                if (found.size() < ef || neighborDist < found.peek().getKey()) {
                    candidates.offer(Map.entry(neighborDist, neighborId));
                    found.offer(Map.entry(neighborDist, neighborId));
                    if (found.size() > ef) found.poll();
                }
            }
        }

        List<Map.Entry<Float, Integer>> results = new ArrayList<>(found);
        results.sort(Map.Entry.comparingByKey());
        return results;
    }

    private List<Integer> selectNeighbors(List<Map.Entry<Float, Integer>> candidates, int maxM) {
        List<Integer> selected = new ArrayList<>();
        for (int i = 0; i < Math.min(candidates.size(), maxM); i++) {
            selected.add(candidates.get(i).getValue());
        }
        return selected;
    }

    private void pruneConnections(Node node, int layer, int maxM, DistanceMetric.DistanceFunction distFn) {
        List<Integer> connections = node.neighbors.get(layer);
        if (connections.size() <= maxM) return;

        List<Map.Entry<Float, Integer>> distances = new ArrayList<>();
        for (int connId : connections) {
            Node connNode = graph.get(connId);
            if (connNode != null) {
                float dist = distFn.calculate(node.item.getEmbedding(), connNode.item.getEmbedding());
                distances.add(Map.entry(dist, connId));
            }
        }

        distances.sort(Map.Entry.comparingByKey());
        connections.clear();
        for (int i = 0; i < maxM && i < distances.size(); i++) {
            connections.add(distances.get(i).getValue());
        }
    }

    public List<Map.Entry<Float, Integer>> knn(float[] query, int k, int ef, DistanceMetric.DistanceFunction distFn) {
        if (entryPoint == -1) return Collections.emptyList();

        int ep = entryPoint;
        for (int lc = topLayer; lc > 0; lc--) {
            List<Map.Entry<Float, Integer>> nearest = searchLayer(query, ep, 1, lc, distFn);
            if (!nearest.isEmpty()) ep = nearest.get(0).getValue();
        }

        List<Map.Entry<Float, Integer>> results = searchLayer(query, ep, Math.max(ef, k), 0, distFn);
        return results.subList(0, Math.min(k, results.size()));
    }

    public void remove(int id) {
        Node node = graph.remove(id);
        if (node == null) return;

        // Remove from all neighbor lists
        for (Node n : graph.values()) {
            for (List<Integer> layer : n.neighbors) {
                layer.remove(Integer.valueOf(id));
            }
        }

        // Update entry point if needed
        if (entryPoint == id && !graph.isEmpty()) {
            entryPoint = graph.keySet().iterator().next();
        }
    }

    public Map<String, Object> getInfo() {
        int maxL = Math.max(topLayer + 1, 1);
        int[] nodesPerLayer = new int[maxL];
        int[] edgesPerLayer = new int[maxL];

        for (Node node : graph.values()) {
            for (int lc = 0; lc <= node.maxLayer && lc < maxL; lc++) {
                nodesPerLayer[lc]++;
                edgesPerLayer[lc] += node.neighbors.get(lc).size();
            }
        }

        Map<String, Object> info = new HashMap<>();
        info.put("topLayer", topLayer);
        info.put("nodeCount", graph.size());
        info.put("nodesPerLayer", nodesPerLayer);
        info.put("edgesPerLayer", edgesPerLayer);
        return info;
    }

    public int size() {
        return graph.size();
    }
}
