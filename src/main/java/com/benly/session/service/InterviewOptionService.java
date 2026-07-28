package com.benly.session.service;

import com.benly.session.dto.InterviewOptionResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class InterviewOptionService {

    public InterviewOptionResponse getOptions(){
        List<InterviewOptionResponse.CompanyType> companyTypes = List.of(
                new InterviewOptionResponse.CompanyType("BIG_TECH_SW", "대기업 SW", "삼성·SK하이닉스·LG CNS"),
                new InterviewOptionResponse.CompanyType("SERVICE", "서비스 기업", "네이버·카카오·쿠팡·배민"),
                new InterviewOptionResponse.CompanyType("FINANCE_IT", "금융 IT", "토스·카카오뱅크·보안/규제")
        );

        List<InterviewOptionResponse.Stage> stages = List.of(
                new InterviewOptionResponse.Stage("TECHNICAL", "1차 기술"),
                new InterviewOptionResponse.Stage("PERSONALITY", "2차 인성·임원")
        );

        List<String> jobRoleChips = List.of("백엔드", "프론트엔", "풀스택");

        return new InterviewOptionResponse(companyTypes, stages, jobRoleChips);
    }
}
