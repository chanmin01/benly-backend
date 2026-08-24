package com.benly.feedback.repository;

import com.benly.session.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionReadRepository extends JpaRepository<Session, Long> {

}
