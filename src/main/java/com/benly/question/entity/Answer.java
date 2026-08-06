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

    @Enumerated(EnumType.STRING)
    @Column(name = "input_type", nullable = false, length = 10)
    private AnswerType inputType;

    @Column(name = "duration_sec")
    private Integer durationSec;

    @Column(name = "stt_status", length = 20)
    private String sttStatus;


    private Answer(Question question, String transcript, AnswerType inputType, Integer durationSec, String sttStatus) {
        this.question = question;
        this.transcript = transcript;
        this.inputType = inputType;
        this.durationSec = durationSec;
        this.sttStatus = sttStatus;
    }

    public static Answer createText(Question question, String transcript){
        return new Answer(question, transcript, AnswerType.TEXT, null, null);
    }


    public static Answer createAudio(Question question, String transcript, Integer durationSec) {
        return new Answer(question, transcript, AnswerType.AUDIO, durationSec, "COMPLETED");
    }
}

