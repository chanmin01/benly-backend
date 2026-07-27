package com.benly.question.entity;

import com.benly.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.EmbeddedColumnNaming;

@Entity
@Getter
@Table(name = "answers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Answer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false, unique = true)
    private Question question;

    @Column(name = "transcript", columnDefinition = "TEXT")
    private String transcript;

    @Column(name = "input_type", nullable = false, length = 10)
    private String inputType;

    @Column(name = "duration_sec")
    private Integer durationSec;

    @Column(name = "stt_status", length = 20)
    private String sttStatus;
}
