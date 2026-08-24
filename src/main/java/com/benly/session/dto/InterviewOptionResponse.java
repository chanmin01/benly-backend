package com.benly.session.dto;

import java.util.List;

public record InterviewOptionResponse(
        List<CompanyType> companyTypes,
        List<Stage> stages,
        List<String> jobRoleChips) {

    public record CompanyType(String code, String label, String example){}
    public record Stage(String code, String label){}
}
