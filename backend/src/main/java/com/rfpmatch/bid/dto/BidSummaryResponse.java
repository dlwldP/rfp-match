package com.rfpmatch.bid.dto;

import com.rfpmatch.bid.domain.BidAnnouncement;
import com.rfpmatch.bid.domain.BidStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 공고 목록 한 줄. 요구사항 등록 건수와 매칭 진행 여부까지 함께 내려 목록에서 다음 할 일이 보이게 한다. */
public record BidSummaryResponse(
        Long bidId,
        String bidNtceNo,
        String title,
        String institution,
        LocalDateTime closeDate,
        BigDecimal estimatedPrice,
        BidStatus status,
        long requirementCount,
        String matchStatus) {

    public static final String MATCHED = "매칭완료";
    public static final String NOT_MATCHED = "미매칭";

    public static BidSummaryResponse of(BidAnnouncement bid, long requirementCount, boolean matched,
                                        LocalDateTime now) {
        return new BidSummaryResponse(
                bid.getId(),
                bid.getBidNtceNo(),
                bid.getTitle(),
                bid.getInstitution(),
                bid.getCloseDate(),
                bid.getEstimatedPrice(),
                bid.status(now),
                requirementCount,
                matched ? MATCHED : NOT_MATCHED);
    }
}
