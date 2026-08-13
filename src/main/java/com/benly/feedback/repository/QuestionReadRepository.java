package com.benly.feedback.repository;

import com.benly.question.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionReadRepository extends JpaRepository<Question, Long> {

    List<Question> findBySession_IdOrderBySeqAsc(Long sessionId);
}
