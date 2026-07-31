package com.benly.question.repository;

import com.benly.question.entity.SeedQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeedQuestionRepository extends JpaRepository<SeedQuestion, Long> {

    List<SeedQuestion> findTop5ByCompanyTypeAndStage(String companyType, String stage);
}
