package com.rfpmatch.requirement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공고 한 건의 RFP에서 뽑아낸 요구사항 한 줄.
 * 첨부파일(HWP/PDF) 원문은 Open API로 구조화되어 오지 않으므로 담당자가 읽고 등록한다.
 */
@Entity
@Table(
        name = "requirement_item",
        indexes = @Index(name = "idx_requirement_bid", columnList = "bid_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RequirementItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bid_id", nullable = false)
    private Long bidId;

    /** 요구사항 분류. 예: 처리성능 / 인증 / 기능 */
    @Column(nullable = false, length = 100)
    private String category;

    /** RFP 원문 문구. 예: "네트워크 처리량 10Gbps 이상" */
    @Column(nullable = false, length = 1000)
    private String description;

    /** 비교 대상 값. 예: "10", "EAL2", "YES" */
    @Column(name = "required_value", nullable = false, length = 200)
    private String requiredValue;

    /** 값의 단위. 없으면 null. 예: "Gbps" */
    @Column(length = 50)
    private String unit;

    /** 필수(mandatory) 요구사항이면 미충족 시 입찰 자체가 불가하다. */
    @Column(nullable = false)
    private boolean mandatory;

    @Builder
    private RequirementItem(Long bidId, String category, String description, String requiredValue,
                            String unit, boolean mandatory) {
        this.bidId = bidId;
        this.category = category;
        this.description = description;
        this.requiredValue = requiredValue;
        this.unit = unit;
        this.mandatory = mandatory;
    }
}
