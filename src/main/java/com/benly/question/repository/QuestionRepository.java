package com.benly.question.repository;

import com.benly.question.entity.Question;
import com.benly.session.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    Optional<Question> findFirstBySessionAndParentIsNullOrderBySeqAsc(Session session);

    int countByParent(Question parent); // 특정 메인의 꼬리 개수 파악

    List<Question> findByParentOrderBySeqAsc(Question parent); // 특정 메인의 꼬리 질문들 -> Claude에게 질문 시 맥락을 만들기 위해

    Optional<Question> findFirstBySessionAndParentIsNullAndSeqGreaterThanOrderBySeqAsc(Session session, Integer seq);
}
