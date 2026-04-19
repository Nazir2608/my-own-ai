package com.nazir.myownai.algorithm;

public class DistanceMetric {

    public static float euclidean(float[] a, float[] b) {
        float sum = 0;
        for (int i = 0; i < a.length; i++) {
            float diff = a[i] - b[i];
            sum += diff * diff;
        }
        return (float) Math.sqrt(sum);
    }

    public static float cosine(float[] a, float[] b) {
        float dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA < 1e-9f || normB < 1e-9f) return 1.0f;
        return 1.0f - (dot / ((float) Math.sqrt(normA) * (float) Math.sqrt(normB)));
    }

    public static float manhattan(float[] a, float[] b) {
        float sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += Math.abs(a[i] - b[i]);
        }
        return sum;
    }

    public static DistanceFunction getFunction(String metric) {
        return switch (metric.toLowerCase()) {
            case "cosine" -> DistanceMetric::cosine;
            case "manhattan" -> DistanceMetric::manhattan;
            default -> DistanceMetric::euclidean;
        };
    }

    @FunctionalInterface
    public interface DistanceFunction {
        float calculate(float[] a, float[] b);
    }
}
