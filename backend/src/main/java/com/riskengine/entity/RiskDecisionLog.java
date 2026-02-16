package com.riskengine.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "risk_decisions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskDecisionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "user_id", length = 100)
    private String userId;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore;

    @Column(name = "decision", length = 20, nullable = false)
    private String decision;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
