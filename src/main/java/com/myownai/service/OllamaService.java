package com.myownai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class OllamaService {

    @Value("${ollama.host:http://localhost:11434}")
    private String ollamaHost;

    @Value("${ollama.embed.model:nomic-embed-text}")
    private String embedModel;

    @Value("${ollama.generate.model:gemma3:4b}")
    private String generateModel;

    @Value("${ollama.timeout.seconds:180}")
    private int timeoutSeconds;

    private WebClient getWebClient() {
        return WebClient.builder()
            .baseUrl(ollamaHost)
            .build();
    }

    public boolean isAvailable() {
        try {
            getWebClient()
                .get()
                .uri("/api/tags")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(2))
                .block();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public float[] embed(String text) {
        try {
            Map<String, Object> request = Map.of(
                "model", embedModel,
                "prompt", text
            );

            Map<String, Object> response = getWebClient()
                .post()
                .uri("/api/embeddings")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .block();

            if (response != null && response.containsKey("embedding")) {
                List<Double> embedding = (List<Double>) response.get("embedding");
                float[] result = new float[embedding.size()];
                for (int i = 0; i < embedding.size(); i++) {
                    result[i] = embedding.get(i).floatValue();
                }
                return result;
            }
        } catch (WebClientResponseException e) {
            System.err.println("Ollama embedding error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Ollama connection error: " + e.getMessage());
        }
        return new float[0];
    }

    public String generate(String prompt) {
        try {
            Map<String, Object> request = Map.of(
                "model", generateModel,
                "prompt", prompt,
                "stream", false
            );

            Map<String, Object> response = getWebClient()
                .post()
                .uri("/api/generate")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .block();

            if (response != null && response.containsKey("response")) {
                return (String) response.get("response");
            }
        } catch (Exception e) {
            return "ERROR: Ollama unavailable. Make sure it's running: ollama serve";
        }
        return "ERROR: No response from Ollama";
    }

    public String getEmbedModel() {
        return embedModel;
    }

    public String getGenerateModel() {
        return generateModel;
    }
}
