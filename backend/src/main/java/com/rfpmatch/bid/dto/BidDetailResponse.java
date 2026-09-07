package com.rfpmatch.bid.dto;

import com.rfpmatch.bid.domain.BidAnnouncement;
import com.rfpmatch.bid.domain.BidStatus;
import com.rfpmatch.requirement.domain.RequirementItem;
import com.rfpmatch.requirement.dto.RequirementItemResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 공고 상세 + 등록된 요구사항 목록. */
public record BidDetailResponse(
        Long bidId,
        String bidNtceNo,
        String title,
        String institution,
        String demandInstitution,
        LocalDateTime closeDate,
        BigDecimal estimatedPrice,
        String noticeUrl,
        BidStatus status,
        LocalDateTime syncedAt,
        List<RequirementItemResponse> requirements) {

    public static BidDetailResponse of(BidAnnouncement bid, List<RequirementItem> requirements, LocalDateTime now) {
        return new BidDetailResponse(
                bid.getId(),
                bid.getBidNtceNo(),
                bid.getTitle(),
                bid.getInstitution(),
                bid.getDemandInstitution(),
                bid.getCloseDate(),
                bid.getEstimatedPrice(),
                bid.getNoticeUrl(),
                bid.status(now),
                bid.getSyncedAt(),
                requirements.stream().map(RequirementItemResponse::from).toList());
    }
}
