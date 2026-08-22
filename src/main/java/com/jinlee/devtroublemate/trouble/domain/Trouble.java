package com.jinlee.devtroublemate.trouble.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.jinlee.devtroublemate.global.domain.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)

public class Trouble extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String rawLog;

    @Enumerated(EnumType.STRING)
    private TroubleStatus status = TroubleStatus.OPEN;

    @Column(columnDefinition = "TEXT")
    private String actualCause;

    @Column(columnDefinition = "TEXT")
    private String solution;
    private String referenceLink;
    private LocalDateTime archivedAt;

    @Builder
    public Trouble(String title, String description, String rawLog) {
        this.title = title;
        this.description = description;
        this.rawLog = rawLog;
        this.status = TroubleStatus.OPEN;
    }

    public void resolve(
            String actualCause,
            String solution,
            String referenceLink
    ) {
        this.actualCause = actualCause;
        this.solution = solution;
        this.referenceLink = referenceLink;
        this.status = TroubleStatus.RESOLVED;
    }

    public void update(
            String title,
            String description,
            String rawLog
    ) {
        this.title = title;
        this.description = description;
        this.rawLog = rawLog;
    }

    public void archive() {
        if (this.archivedAt == null) {
            this.archivedAt = LocalDateTime.now();
        }
    }

    public void restore() {
        this.archivedAt = null;
    }

}
