CREATE INDEX IF NOT EXISTS idx_trouble_embedding_embedding_hnsw_cosine
    ON trouble_embedding
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);
