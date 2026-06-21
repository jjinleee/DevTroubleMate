package com.jinlee.devtroublemate.trouble.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)

public class Trouble {

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

}