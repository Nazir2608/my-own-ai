package com.nazir.myownai.model;

import java.util.List;

public class SearchResult {
    private List<Hit> results;
    private long latencyUs;
    private String algo;
    private String metric;

    public static class Hit {
        private int id;
        private String metadata;
        private String category;
        private float[] embedding;
        private float distance;

        public Hit(int id, String metadata, String category, float[] embedding, float distance) {
            this.id = id;
            this.metadata = metadata;
            this.category = category;
            this.embedding = embedding;
            this.distance = distance;
        }

        // Getters
        public int getId() { return id; }
        public String getMetadata() { return metadata; }
        public String getCategory() { return category; }
        public float[] getEmbedding() { return embedding; }
        public float getDistance() { return distance; }
    }

    public SearchResult(List<Hit> results, long latencyUs, String algo, String metric) {
        this.results = results;
        this.latencyUs = latencyUs;
        this.algo = algo;
        this.metric = metric;
    }

    // Getters
    public List<Hit> getResults() { return results; }
    public long getLatencyUs() { return latencyUs; }
    public String getAlgo() { return algo; }
    public String getMetric() { return metric; }
}
