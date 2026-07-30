package com.benly.session.service;

import com.benly.session.dto.InterviewOptionResponse;
import com.benly.session.entity.CompanyType;
import com.benly.session.entity.InterviewStage;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class InterviewOptionService {

    public InterviewOptionResponse getOptions(){
        List<InterviewOptionResponse.CompanyType> companyTypes =
                Arrays.stream(CompanyType.values())
                        .map(c -> new InterviewOptionResponse.CompanyType(
                                c.name(), c.getLabel(), c.getExample()))
                        .toList();

        List<InterviewOptionResponse.Stage> stages =
                Arrays.stream(InterviewStage.values())
                .map(s -> new InterviewOptionResponse.Stage(
                        s.name(), s.getLabel()))
                .toList();

        List<String> jobRoleChips = List.of("백엔드", "프론트엔드", "풀스택");

        return new InterviewOptionResponse(companyTypes, stages, jobRoleChips);
    }
}
