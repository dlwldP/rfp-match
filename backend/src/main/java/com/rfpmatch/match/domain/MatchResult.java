package com.rfpmatch.match.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 매칭 실행 1회에서 나온 요구사항 1건의 판정 결과(갭분석 한 줄). */
@Entity
@Table(
        name = "match_result",
        indexes = @Index(name = "idx_match_bid_product", columnList = "bid_id,product_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bid_id", nullable = false)
    private Long bidId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "requirement_id", nullable = false)
    private Long requirementId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchStatus status;

    /** 요구값과 비교한 제품 스펙 값. 비교 대상 스펙이 없으면 null. */
    @Column(name = "matched_value", length = 500)
    private String matchedValue;

    /** 판정 근거. 예: "20Gbps >= 10Gbps" */
    @Column(length = 1000)
    private String note;

    @Column(name = "matched_at", nullable = false)
    private LocalDateTime matchedAt;

    @Builder
    private MatchResult(Long bidId, Long productId, Long requirementId, MatchStatus status,
                        String matchedValue, String note, LocalDateTime matchedAt) {
        this.bidId = bidId;
        this.productId = productId;
        this.requirementId = requirementId;
        this.status = status;
        this.matchedValue = matchedValue;
        this.note = note;
        this.matchedAt = matchedAt != null ? matchedAt : LocalDateTime.now();
    }
}
