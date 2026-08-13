package com.benly.feedback.repository;

import com.benly.feedback.entity.SessionFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SessionFeedbackRepository extends JpaRepository<SessionFeedback, Long> {

    Optional<SessionFeedback> findBySession_Id(Long sessionId);

    boolean existsBySession_Id(Long sessionId);
}
