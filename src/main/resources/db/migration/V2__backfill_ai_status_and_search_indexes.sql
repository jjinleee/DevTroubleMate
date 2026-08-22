CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

ALTER TABLE trouble
    ADD COLUMN IF NOT EXISTS archived_at TIMESTAMP(6),
    ADD COLUMN IF NOT EXISTS ai_processing_status VARCHAR(255),
    ADD COLUMN IF NOT EXISTS ai_last_error_code VARCHAR(255),
    ADD COLUMN IF NOT EXISTS ai_last_error_message TEXT,
    ADD COLUMN IF NOT EXISTS ai_processed_at TIMESTAMP(6);

ALTER TABLE aianalysis
    ADD COLUMN IF NOT EXISTS model_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS prompt_version VARCHAR(255);

UPDATE trouble t
SET ai_processing_status = CASE
        WHEN EXISTS (SELECT 1 FROM aianalysis a WHERE a.trouble_id = t.id)
             AND EXISTS (SELECT 1 FROM trouble_embedding e WHERE e.trouble_id = t.id)
            THEN 'COMPLETED'
        WHEN EXISTS (SELECT 1 FROM aianalysis a WHERE a.trouble_id = t.id)
             OR EXISTS (SELECT 1 FROM trouble_embedding e WHERE e.trouble_id = t.id)
            THEN 'FAILED'
        ELSE 'PENDING'
    END,
    ai_last_error_code = CASE
        WHEN EXISTS (SELECT 1 FROM aianalysis a WHERE a.trouble_id = t.id)
             <> EXISTS (SELECT 1 FROM trouble_embedding e WHERE e.trouble_id = t.id)
            THEN 'MIGRATION_INCOMPLETE_AI_DATA'
        ELSE ai_last_error_code
    END,
    ai_last_error_message = CASE
        WHEN EXISTS (SELECT 1 FROM aianalysis a WHERE a.trouble_id = t.id)
             <> EXISTS (SELECT 1 FROM trouble_embedding e WHERE e.trouble_id = t.id)
            THEN '기존 AI 분석 또는 임베딩 데이터가 불완전하여 재시도가 필요합니다.'
        ELSE ai_last_error_message
    END,
    ai_processed_at = CASE
        WHEN EXISTS (SELECT 1 FROM aianalysis a WHERE a.trouble_id = t.id)
          OR EXISTS (SELECT 1 FROM trouble_embedding e WHERE e.trouble_id = t.id)
            THEN COALESCE(
                    (SELECT e.updated_at FROM trouble_embedding e WHERE e.trouble_id = t.id),
                    (SELECT a.updated_at FROM aianalysis a WHERE a.trouble_id = t.id),
                    t.updated_at,
                    CURRENT_TIMESTAMP
            )
        ELSE ai_processed_at
    END
WHERE t.ai_processing_status IS NULL;

ALTER TABLE trouble
    ALTER COLUMN ai_processing_status SET DEFAULT 'PENDING';

UPDATE trouble
SET ai_processing_status = 'PENDING'
WHERE ai_processing_status IS NULL;

ALTER TABLE trouble
    ALTER COLUMN ai_processing_status SET NOT NULL;

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
    ON trouble USING gin (lower(title) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_trouble_description_trgm
    ON trouble USING gin (lower(description) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_trouble_tag_trouble_id
    ON trouble_tag (trouble_id);

CREATE INDEX IF NOT EXISTS idx_trouble_tag_tag_id_trouble_id
    ON trouble_tag (tag_id, trouble_id);
