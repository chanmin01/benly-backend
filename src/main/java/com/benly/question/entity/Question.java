package com.benly.question.entity;

import com.benly.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "questions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Question extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Question parent;

    @Column(name = "type", nullable = false, length = 20)
    private String type;

    @Column(name = "strategy", length = 20)
    private String strategy;

    @Column(name = "seq", nullable = false)
    private Integer seq;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "short_title", columnDefinition = "TEXT")
    private String shortTitle;
}
