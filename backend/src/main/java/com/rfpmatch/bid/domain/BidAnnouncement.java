package com.rfpmatch.bid.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 외부 Open API로 수집한 입찰공고. 공고번호(bidNtceNo)가 자연키이며,
 * 스케줄러가 같은 공고를 다시 내려받으면 새 행을 만들지 않고 이 엔티티를 갱신한다.
 */
@Entity
@Table(
        name = "bid_announcement",
        uniqueConstraints = @UniqueConstraint(name = "uk_bid_ntce_no", columnNames = "bid_ntce_no")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BidAnnouncement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bid_ntce_no", nullable = false, length = 50)
    private String bidNtceNo;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 200)
    private String institution;

    @Column(name = "demand_institution", length = 200)
    private String demandInstitution;

    @Column(name = "close_date")
    private LocalDateTime closeDate;

    @Column(name = "estimated_price", precision = 19, scale = 0)
    private BigDecimal estimatedPrice;

    @Column(name = "notice_url", length = 1000)
    private String noticeUrl;

    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;

    @Builder
    private BidAnnouncement(String bidNtceNo, String title, String institution, String demandInstitution,
                            LocalDateTime closeDate, BigDecimal estimatedPrice, String noticeUrl,
                            LocalDateTime syncedAt) {
        this.bidNtceNo = bidNtceNo;
        this.title = title;
        this.institution = institution;
        this.demandInstitution = demandInstitution;
        this.closeDate = closeDate;
        this.estimatedPrice = estimatedPrice;
        this.noticeUrl = noticeUrl;
        this.syncedAt = syncedAt != null ? syncedAt : LocalDateTime.now();
    }

    /** 외부 API에서 다시 내려받은 내용으로 갱신한다. */
    public void syncFrom(BidAnnouncement fresh) {
        this.title = fresh.title;
        this.institution = fresh.institution;
        this.demandInstitution = fresh.demandInstitution;
        this.closeDate = fresh.closeDate;
        this.estimatedPrice = fresh.estimatedPrice;
        this.noticeUrl = fresh.noticeUrl;
        this.syncedAt = fresh.syncedAt != null ? fresh.syncedAt : LocalDateTime.now();
    }

    /** 투찰 마감이 지나지 않았으면 진행중(OPEN). */
    public BidStatus status(LocalDateTime now) {
        if (closeDate == null) {
            return BidStatus.UNKNOWN;
        }
        return closeDate.isAfter(now) ? BidStatus.OPEN : BidStatus.CLOSED;
    }
}
