package com.nazir.myownai.repository;

import com.nazir.myownai.entity.VectorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VectorRepository extends JpaRepository<VectorEntity, Integer> {

    // Find by category
    List<VectorEntity> findByCategory(String category);

    // Cosine similarity search (pgvector)
    @Query(value = "SELECT v.*, 1 - (v.embedding <=> CAST(:query AS vector)) AS similarity " +
            "FROM demo_vectors v " +
            "ORDER BY v.embedding <=> CAST(:query AS vector) " +
            "LIMIT :k",
            nativeQuery = true)
    List<Object[]> findNearestCosine(@Param("query") String query, @Param("k") int k);

    // Euclidean distance search (pgvector)
    @Query(value = "SELECT v.*, v.embedding <-> CAST(:query AS vector) AS distance " +
            "FROM demo_vectors v " +
            "ORDER BY v.embedding <-> CAST(:query AS vector) " +
            "LIMIT :k",
            nativeQuery = true)
    List<Object[]> findNearestEuclidean(@Param("query") String query, @Param("k") int k);

    // Count by category
    long countByCategory(String category);
}