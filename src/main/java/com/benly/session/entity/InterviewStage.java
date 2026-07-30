package com.benly.session.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InterviewStage {

    TECHNICAL("1차 기술"),
    PERSONALITY("2차 인성·임원");

    private final String label;
}
