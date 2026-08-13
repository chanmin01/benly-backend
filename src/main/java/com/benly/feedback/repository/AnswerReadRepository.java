package com.benly.feedback.repository;

import com.benly.question.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnswerReadRepository extends JpaRepository<Answer, Long> {

    @Query("SELECT a FROM Answer a WHERE a.question.session.id = :sessionId")
    List<Answer> findBySessionId(@Param("sessionId") Long sessionId);

}
