ALTER TABLE trouble
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE trouble_tag
    ADD CONSTRAINT uk_trouble_tag_trouble_id_tag_id
        UNIQUE (trouble_id, tag_id);
