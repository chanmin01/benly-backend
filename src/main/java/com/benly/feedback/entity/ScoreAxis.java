package com.benly.feedback.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "score_axes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScoreAxis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feedback_id", nullable = false)
    private Feedback feedback;

    @Column(name = "axis", nullable = false, length = 30)
    private String axis;

    @Column(name = "score", nullable = false)
    private Integer score;

    private ScoreAxis(Feedback feedback, String axis, Integer score) {
        this.feedback = feedback;
        this.axis = axis;
        this.score = score;
    }

    public static ScoreAxis create(Feedback feedback, String axis, Integer score) {
        return new ScoreAxis(feedback, axis, score);
    }
}
