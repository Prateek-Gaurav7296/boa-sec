package com.riskengine.repository;

import com.riskengine.entity.RiskDecisionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RiskDecisionRepository extends JpaRepository<RiskDecisionLog, Long> {

    List<RiskDecisionLog> findBySessionId(String sessionId);
}
