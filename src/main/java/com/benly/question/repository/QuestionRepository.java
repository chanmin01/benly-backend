package com.benly.question.repository;

import com.benly.question.entity.Question;
import com.benly.session.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    Optional<Question> findFirstBySessionAndParentIsNullOrderBySeqAsc(Session session);
}
