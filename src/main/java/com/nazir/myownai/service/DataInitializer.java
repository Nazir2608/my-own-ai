package com.nazir.myownai.service;

import com.nazir.myownai.repository.VectorRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DataInitializer {

    @Autowired
    private VectorRepository vectorRepository;

    @Autowired
    private VectorDBService vectorDBService;

    @PostConstruct
    public void loadDemoData() {
        // Only load demo data if database is empty
        if (vectorRepository.count() == 0) {

            System.out.println("Loading demo data into database...");

            // CS / Algorithms
            vectorDBService.insert("Linked List: nodes connected by pointers", "cs",
                    new float[]{0.90f, 0.85f, 0.72f, 0.68f, 0.12f, 0.08f, 0.15f, 0.10f, 0.05f, 0.08f, 0.06f, 0.09f, 0.07f, 0.11f, 0.08f, 0.06f});
            vectorDBService.insert("Binary Search Tree: O(log n) search and insert", "cs",
                    new float[]{0.88f, 0.82f, 0.78f, 0.74f, 0.15f, 0.10f, 0.08f, 0.12f, 0.06f, 0.07f, 0.08f, 0.05f, 0.09f, 0.06f, 0.07f, 0.10f});
            vectorDBService.insert("Dynamic Programming: memoization overlapping subproblems", "cs",
                    new float[]{0.82f, 0.76f, 0.88f, 0.80f, 0.20f, 0.18f, 0.12f, 0.09f, 0.07f, 0.06f, 0.08f, 0.07f, 0.08f, 0.09f, 0.06f, 0.07f});
            vectorDBService.insert("Graph BFS and DFS: breadth and depth first traversal", "cs",
                    new float[]{0.85f, 0.80f, 0.75f, 0.82f, 0.18f, 0.14f, 0.10f, 0.08f, 0.06f, 0.09f, 0.07f, 0.06f, 0.10f, 0.08f, 0.09f, 0.07f});
            vectorDBService.insert("Hash Table: O(1) lookup with collision chaining", "cs",
                    new float[]{0.87f, 0.78f, 0.70f, 0.76f, 0.13f, 0.11f, 0.09f, 0.14f, 0.08f, 0.07f, 0.06f, 0.08f, 0.07f, 0.10f, 0.08f, 0.09f});

            // Mathematics
            vectorDBService.insert("Calculus: derivatives integrals and limits", "math",
                    new float[]{0.12f, 0.15f, 0.18f, 0.10f, 0.91f, 0.86f, 0.78f, 0.72f, 0.08f, 0.06f, 0.07f, 0.09f, 0.07f, 0.08f, 0.06f, 0.10f});
            vectorDBService.insert("Linear Algebra: matrices eigenvalues eigenvectors", "math",
                    new float[]{0.20f, 0.18f, 0.15f, 0.12f, 0.88f, 0.90f, 0.82f, 0.76f, 0.09f, 0.07f, 0.08f, 0.06f, 0.10f, 0.07f, 0.08f, 0.09f});
            vectorDBService.insert("Probability: distributions random variables Bayes theorem", "math",
                    new float[]{0.15f, 0.12f, 0.20f, 0.18f, 0.84f, 0.80f, 0.88f, 0.82f, 0.07f, 0.08f, 0.06f, 0.10f, 0.09f, 0.06f, 0.09f, 0.08f});
            vectorDBService.insert("Number Theory: primes modular arithmetic RSA cryptography", "math",
                    new float[]{0.22f, 0.16f, 0.14f, 0.20f, 0.80f, 0.85f, 0.76f, 0.90f, 0.08f, 0.09f, 0.07f, 0.06f, 0.08f, 0.10f, 0.07f, 0.06f});
            vectorDBService.insert("Combinatorics: permutations combinations generating functions", "math",
                    new float[]{0.18f, 0.20f, 0.16f, 0.14f, 0.86f, 0.78f, 0.84f, 0.80f, 0.06f, 0.07f, 0.09f, 0.08f, 0.06f, 0.09f, 0.10f, 0.07f});

            // Food
            vectorDBService.insert("Neapolitan Pizza: wood-fired dough San Marzano tomatoes", "food",
                    new float[]{0.08f, 0.06f, 0.09f, 0.07f, 0.07f, 0.08f, 0.06f, 0.09f, 0.90f, 0.86f, 0.78f, 0.72f, 0.08f, 0.06f, 0.09f, 0.07f});
            vectorDBService.insert("Sushi: vinegared rice raw fish and nori rolls", "food",
                    new float[]{0.06f, 0.08f, 0.07f, 0.09f, 0.09f, 0.06f, 0.08f, 0.07f, 0.86f, 0.90f, 0.82f, 0.76f, 0.07f, 0.09f, 0.06f, 0.08f});
            vectorDBService.insert("Ramen: noodle soup with chashu pork and soft-boiled eggs", "food",
                    new float[]{0.09f, 0.07f, 0.06f, 0.08f, 0.08f, 0.09f, 0.07f, 0.06f, 0.82f, 0.78f, 0.90f, 0.84f, 0.09f, 0.07f, 0.08f, 0.06f});
            vectorDBService.insert("Tacos: corn tortillas with carnitas salsa and cilantro", "food",
                    new float[]{0.07f, 0.09f, 0.08f, 0.06f, 0.06f, 0.07f, 0.09f, 0.08f, 0.78f, 0.82f, 0.86f, 0.90f, 0.06f, 0.08f, 0.07f, 0.09f});
            vectorDBService.insert("Croissant: laminated pastry with buttery flaky layers", "food",
                    new float[]{0.06f, 0.07f, 0.10f, 0.09f, 0.10f, 0.06f, 0.07f, 0.10f, 0.85f, 0.80f, 0.76f, 0.82f, 0.09f, 0.07f, 0.10f, 0.06f});

            // Sports
            vectorDBService.insert("Basketball: fast-paced shooting dribbling slam dunks", "sports",
                    new float[]{0.09f, 0.07f, 0.08f, 0.10f, 0.08f, 0.09f, 0.07f, 0.06f, 0.08f, 0.07f, 0.09f, 0.06f, 0.91f, 0.85f, 0.78f, 0.72f});
            vectorDBService.insert("Football: tackles touchdowns field goals and strategy", "sports",
                    new float[]{0.07f, 0.09f, 0.06f, 0.08f, 0.09f, 0.07f, 0.10f, 0.08f, 0.07f, 0.09f, 0.08f, 0.07f, 0.87f, 0.89f, 0.82f, 0.76f});
            vectorDBService.insert("Tennis: racket volleys groundstrokes and Wimbledon serves", "sports",
                    new float[]{0.08f, 0.06f, 0.09f, 0.07f, 0.07f, 0.08f, 0.06f, 0.09f, 0.09f, 0.06f, 0.07f, 0.08f, 0.83f, 0.80f, 0.88f, 0.82f});
            vectorDBService.insert("Chess: openings endgames tactics strategic board game", "sports",
                    new float[]{0.25f, 0.20f, 0.22f, 0.18f, 0.22f, 0.18f, 0.20f, 0.15f, 0.06f, 0.08f, 0.07f, 0.09f, 0.80f, 0.84f, 0.78f, 0.90f});
            vectorDBService.insert("Swimming: butterfly freestyle backstroke Olympic competition", "sports",
                    new float[]{0.06f, 0.08f, 0.07f, 0.09f, 0.08f, 0.06f, 0.09f, 0.07f, 0.10f, 0.08f, 0.06f, 0.07f, 0.85f, 0.82f, 0.86f, 0.80f});

            System.out.println("Loaded 20 demo vectors into PostgreSQL");
        } else {
            System.out.println("Using existing data from PostgreSQL (" + vectorRepository.count() + " vectors)");
        }
    }
}