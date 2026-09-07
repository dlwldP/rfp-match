package com.rfpmatch.bid.service;

import com.rfpmatch.bid.domain.BidAnnouncement;
import com.rfpmatch.bid.dto.BidDetailResponse;
import com.rfpmatch.bid.dto.BidSummaryResponse;
import com.rfpmatch.bid.repository.BidAnnouncementRepository;
import com.rfpmatch.common.dto.PageResponse;
import com.rfpmatch.common.exception.NotFoundException;
import com.rfpmatch.match.repository.MatchResultRepository;
import com.rfpmatch.requirement.repository.RequirementItemRepository;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BidService {

    private final BidAnnouncementRepository bidRepository;
    private final RequirementItemRepository requirementRepository;
    private final MatchResultRepository matchResultRepository;

    public BidService(BidAnnouncementRepository bidRepository,
                      RequirementItemRepository requirementRepository,
                      MatchResultRepository matchResultRepository) {
        this.bidRepository = bidRepository;
        this.requirementRepository = requirementRepository;
        this.matchResultRepository = matchResultRepository;
    }

    public PageResponse<BidSummaryResponse> search(String keyword, String status, Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        Page<BidAnnouncement> page = bidRepository.search(
                blankToNull(keyword), blankToNull(status), now, pageable);

        List<Long> bidIds = page.getContent().stream().map(BidAnnouncement::getId).toList();
        // 목록 한 줄마다 count 쿼리가 나가지 않도록 두 번의 조회로 끝낸다.
        Map<Long, Long> requirementCounts = bidIds.isEmpty() ? Map.of()
                : requirementRepository.countByBidIds(bidIds).stream()
                    .collect(Collectors.toMap(
                            RequirementItemRepository.BidRequirementCount::getBidId,
                            RequirementItemRepository.BidRequirementCount::getCount));
        Set<Long> matchedBidIds = bidIds.isEmpty() ? Set.of()
                : new HashSet<>(matchResultRepository.findMatchedBidIds(bidIds));

        List<BidSummaryResponse> content = page.getContent().stream()
                .map(bid -> BidSummaryResponse.of(
                        bid,
                        requirementCounts.getOrDefault(bid.getId(), 0L),
                        matchedBidIds.contains(bid.getId()),
                        now))
                .toList();
        return PageResponse.of(page, content);
    }

    public BidDetailResponse findDetail(Long bidId) {
        BidAnnouncement bid = getBid(bidId);
        return BidDetailResponse.of(bid, requirementRepository.findByBidIdOrderByIdAsc(bidId), LocalDateTime.now());
    }

    public BidAnnouncement getBid(Long bidId) {
        return bidRepository.findById(bidId).orElseThrow(() -> NotFoundException.bid(bidId));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** 테스트/시드용. 운영에서는 스케줄러가 채운다. */
    @Transactional
    public BidAnnouncement save(BidAnnouncement bid) {
        return bidRepository.save(bid);
    }
}
