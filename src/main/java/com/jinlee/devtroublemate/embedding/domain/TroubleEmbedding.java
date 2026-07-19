package com.jinlee.devtroublemate.embedding.domain;

import com.jinlee.devtroublemate.global.domain.BaseEntity;
import com.jinlee.devtroublemate.trouble.domain.Trouble;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "trouble_embedding",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_trouble_embedding_trouble_id",
                        columnNames = "trouble_id"
                )
        }
)
public class TroubleEmbedding extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trouble_id", nullable = false)
    private Trouble trouble;

    @Column(name = "input_text", columnDefinition = "TEXT", nullable = false)
    private String inputText;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 1536)
    @Column(
            name = "embedding",
            columnDefinition = "vector(1536)",
            nullable = false
    )
    private float[] embedding;

    @Column(name = "model_name", nullable = false)
    private String modelName;

    @Builder
    public TroubleEmbedding(
            Trouble trouble,
            String inputText,
            float[] embedding,
            String modelName
    ) {
        this.trouble = trouble;
        this.inputText = inputText;
        this.embedding = embedding;
        this.modelName = modelName;
    }

    public void update(
            String inputText,
            float[] embedding,
            String modelName
    ) {
        this.inputText = inputText;
        this.embedding = embedding;
        this.modelName = modelName;
    }
}