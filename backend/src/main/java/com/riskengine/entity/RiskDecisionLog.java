package com.riskengine.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;

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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "flagged_issues", columnDefinition = "jsonb")
    private List<Map<String, Object>> flaggedIssues;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
