package com.riskengine.repository;

import com.riskengine.entity.RawSignal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RawSignalRepository extends JpaRepository<RawSignal, Long> {

    List<RawSignal> findBySessionId(String sessionId);
}
