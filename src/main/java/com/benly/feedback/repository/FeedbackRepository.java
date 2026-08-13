package com.benly.feedback.repository;

import com.benly.feedback.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    @Query("SELECT f FROM Feedback f JOIN FETCH f.question q " +
            "WHERE q.session.id = :sessionId ORDER BY q.seq ASC")
    List<Feedback> findWithQuestionBySessionId(@Param("sessionId") Long sessionId);

    @Modifying
    @Query("DELETE FROM Feedback f WHERE f.question.session.id = :sessionId")
    void deleteBySessionId(@Param("sessionId") Long sessionId);
}
