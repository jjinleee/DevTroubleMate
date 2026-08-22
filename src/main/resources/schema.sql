CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_trouble_embedding_embedding_hnsw_cosine
    ON trouble_embedding
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

CREATE INDEX IF NOT EXISTS idx_trouble_active_created_at
    ON trouble (created_at DESC)
    WHERE archived_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_trouble_active_status_created_at
    ON trouble (status, created_at DESC)
    WHERE archived_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_trouble_archived_created_at
    ON trouble (created_at DESC)
    WHERE archived_at IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_trouble_title_trgm
    ON trouble
    USING gin (lower(title) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_trouble_description_trgm
    ON trouble
    USING gin (lower(description) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_trouble_tag_trouble_id
    ON trouble_tag (trouble_id);

CREATE INDEX IF NOT EXISTS idx_trouble_tag_tag_id_trouble_id
    ON trouble_tag (tag_id, trouble_id);
