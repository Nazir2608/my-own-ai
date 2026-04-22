package com.nazir.myownai.repository;

import com.nazir.myownai.entity.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, Integer> {

    // Cosine similarity search for documents
    @Query(value = "SELECT d.*, 1 - (d.embedding <=> CAST(:query AS vector)) AS similarity " +
            "FROM documents d " +
            "ORDER BY d.embedding <=> CAST(:query AS vector) " +
            "LIMIT :k",
            nativeQuery = true)
    List<Object[]> findNearestCosine(@Param("query") String query, @Param("k") int k);

    // Find by title (partial match)
    List<DocumentEntity> findByTitleContainingIgnoreCase(String title);

    // Count total documents
    @Query("SELECT COUNT(d) FROM DocumentEntity d")
    long countDocuments();
}