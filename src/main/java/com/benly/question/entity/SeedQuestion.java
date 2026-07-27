package com.benly.question.entity;

import com.benly.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "seed_questions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeedQuestion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_type", nullable = false, length = 20)
    private String companyType;

    @Column(name = "stage", nullable = false, length = 20)
    private String stage;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
}
