package com.jinlee.devtroublemate.ai.domain;

import com.jinlee.devtroublemate.global.domain.BaseEntity;
import com.jinlee.devtroublemate.trouble.domain.Trouble;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AIAnalysis extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trouble_id", nullable = false, unique = true)
    private Trouble trouble;

    private String category;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String possibleCauses;

    @Column(columnDefinition = "TEXT")
    private String runbook;

    private Integer confidence;
    private String modelName;
    private String promptVersion;

    @Builder
    public AIAnalysis(
            Trouble trouble,
            String category,
            String summary,
            String possibleCauses,
            String runbook,
            Integer confidence,
            String modelName,
            String promptVersion
    ) {
        this.trouble = trouble;
        this.category = category;
        this.summary = summary;
        this.possibleCauses = possibleCauses;
        this.runbook = runbook;
        this.confidence = confidence;
        this.modelName = modelName;
        this.promptVersion = promptVersion;
    }

    public void update(
            String category,
            String summary,
            String possibleCauses,
            String runbook,
            Integer confidence,
            String modelName,
            String promptVersion
    ) {
        this.category = category;
        this.summary = summary;
        this.possibleCauses = possibleCauses;
        this.runbook = runbook;
        this.confidence = confidence;
        this.modelName = modelName;
        this.promptVersion = promptVersion;
    }
}
