package com.benly.feedback.dto;

import com.benly.session.entity.Session;

public record ScoringContext(
        String companyType,
        String stage,
        String jobTitle
) {
    public static ScoringContext from(Session session) {
        return new ScoringContext(
                session.getCompanyType(), session.getStage(), session.getJobTitle());
    }
}