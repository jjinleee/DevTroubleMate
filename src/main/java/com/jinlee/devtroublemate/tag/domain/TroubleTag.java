package com.jinlee.devtroublemate.tag.domain;

import com.jinlee.devtroublemate.trouble.domain.Trouble;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TroubleTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trouble_id", nullable = false)
    private Trouble trouble;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;


    @Builder
    public TroubleTag(Trouble trouble, Tag tag) {
        this.trouble = trouble;
        this.tag = tag;
    }
}