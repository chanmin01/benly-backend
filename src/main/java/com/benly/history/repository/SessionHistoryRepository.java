package com.benly.history.repository;

import com.benly.feedback.entity.FeedbackStatus;
import com.benly.session.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SessionHistoryRepository extends JpaRepository<Session, Long> {

    // 채점이 완료(FeedbackStatus.COMPLETED)된 세션만 조회 (세션 ↔ 채점기록 조인)
    @Query("""
            SELECT s FROM Session s
            JOIN SessionFeedback sf ON sf.session = s
            WHERE s.user.id = :userId AND sf.status = :status
            ORDER BY s.createdAt DESC
            """)
    List<Session> findScoredSessions(@Param("userId") Long userId,
                                     @Param("status") FeedbackStatus status);

    @Query("""
            SELECT s FROM Session s
            JOIN SessionFeedback sf ON sf.session = s
            WHERE s.user.id = :userId AND s.companyType = :companyType AND sf.status = :status
            ORDER BY s.createdAt DESC
            """)
    List<Session> findScoredSessionsByCompany(@Param("userId") Long userId,
                                              @Param("companyType") String companyType,
                                              @Param("status") FeedbackStatus status);

    @Query("""
            SELECT COUNT(s) FROM Session s
            JOIN SessionFeedback sf ON sf.session = s
            WHERE s.user.id = :userId AND sf.status = :status
            """)
    long countScored(@Param("userId") Long userId,
                     @Param("status") FeedbackStatus status);

    @Query("""
            SELECT COUNT(s) FROM Session s
            JOIN SessionFeedback sf ON sf.session = s
            WHERE s.user.id = :userId AND sf.status = :status AND s.createdAt >= :from
            """)
    long countScoredSince(@Param("userId") Long userId,
                          @Param("status") FeedbackStatus status,
                          @Param("from") LocalDateTime from);
}