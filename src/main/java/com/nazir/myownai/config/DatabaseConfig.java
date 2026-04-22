package com.nazir.myownai.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initDatabase() {
        try {
            // Create pgvector extension if it doesn't exist
            logger.info("Creating pgvector extension if not exists...");
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
            logger.info("pgvector extension ready");

            // Create indexes for faster searches
            logger.info("Creating vector indexes...");

            // Index for demo_vectors (16D)
            jdbcTemplate.execute(
                    "CREATE INDEX IF NOT EXISTS demo_vectors_embedding_idx ON demo_vectors " +
                            "USING ivfflat (embedding vector_cosine_ops) WITH (lists = 10)"
            );

            // Index for documents (768D)
            jdbcTemplate.execute(
                    "CREATE INDEX IF NOT EXISTS documents_embedding_idx ON documents " +
                            "USING ivfflat (embedding vector_cosine_ops) WITH (lists = 50)"
            );

            logger.info("Vector indexes created");
        } catch (Exception e) {
            logger.error("Failed to initialize database: " + e.getMessage());
        }
    }
}