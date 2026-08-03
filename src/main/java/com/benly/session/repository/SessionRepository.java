package com.benly.session.repository;

import com.benly.session.entity.Session;
import com.benly.session.entity.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionRepository extends JpaRepository<Session, Long> {

    @Modifying
    @Query("UPDATE Session s SET s.status = :newStatus " +
            "WHERE s.id = :sessionId AND s.status = :currentStatus")
    int updateStatusIfCurrent(@Param("sessionId") Long sessionId,
                              @Param("currentStatus") SessionStatus currentStatus,
                              @Param("newStatus") SessionStatus newStatus);
}
