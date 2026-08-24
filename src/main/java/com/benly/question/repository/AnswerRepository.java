package com.benly.question.repository;

import com.benly.question.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    boolean existsByQuestionId(Long questionId);

    Optional<Answer> findByQuestionId(Long questionId);
}
