package com.jinlee.devtroublemate.retrospective.domain;

import com.jinlee.devtroublemate.global.domain.BaseEntity;
import com.jinlee.devtroublemate.trouble.domain.Trouble;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Retrospective extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trouble_id", nullable = false, unique = true)
    private Trouble trouble;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String lesson;

    @Builder
    public Retrospective(Trouble trouble, String content, String lesson) {
        this.trouble = trouble;
        this.content = content;
        this.lesson = lesson;
    }

    public void update(String content, String lesson) {
        this.content = content;
        this.lesson = lesson;
    }
}
