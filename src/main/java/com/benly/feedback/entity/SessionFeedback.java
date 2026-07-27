package com.benly.feedback.entity;

import com.benly.global.entity.BaseEntity;
import com.benly.session.entity.Session;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "session_feedbacks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SessionFeedback extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private Session session;

    @Column(name = "total_score")
    private Integer totalScore;

    @Column(name = "summary", length = 500)
    private String summary;

    @Column(name = "key_coaching", columnDefinition = "TEXT")
    private String keyCoaching;

    @Column(name = "coaching_action", columnDefinition = "TEXT")
    private String coachingAction;

    @Column(name = "filler_word_count")
    private Integer fillerWordCount;

    @Column(name = "filler_word_note", length = 200)
    private String fillerWordNote;

    @Column(name = "speech_speed")
    private Integer speechSpeed;

    @Column(name = "speech_speed_note", length = 200)
    private String speechSpeedNote;

    @Column(name = "status", nullable = false, length = 20)
    private String status;
}
