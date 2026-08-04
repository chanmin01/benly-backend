package com.benly.history.repository;

import com.benly.session.entity.Session;
import com.benly.session.entity.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SessionHistoryRepository extends JpaRepository<Session, Long> {

    List<Session> findByUser_IdAndStatusOrderByCreatedAtDesc(
            Long userId, SessionStatus status);

    List<Session> findByUser_IdAndStatusAndCompanyTypeOrderByCreatedAtDesc(
            Long userId, SessionStatus status, String companyType);

    long countByUser_IdAndStatus(Long userId, SessionStatus status);

    long countByUser_IdAndStatusAndCreatedAtGreaterThanEqual(
            Long userId, SessionStatus status, LocalDateTime from);


}
