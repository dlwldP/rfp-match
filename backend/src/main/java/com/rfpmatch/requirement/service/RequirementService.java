package com.rfpmatch.requirement.service;

import com.rfpmatch.bid.repository.BidAnnouncementRepository;
import com.rfpmatch.common.exception.NotFoundException;
import com.rfpmatch.match.repository.MatchResultRepository;
import com.rfpmatch.requirement.domain.RequirementItem;
import com.rfpmatch.requirement.dto.RequirementCreateRequest;
import com.rfpmatch.requirement.dto.RequirementCreateResponse;
import com.rfpmatch.requirement.dto.RequirementItemResponse;
import com.rfpmatch.requirement.repository.RequirementItemRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RequirementService {

    private final RequirementItemRepository requirementRepository;
    private final BidAnnouncementRepository bidRepository;
    private final MatchResultRepository matchResultRepository;

    public RequirementService(RequirementItemRepository requirementRepository,
                              BidAnnouncementRepository bidRepository,
                              MatchResultRepository matchResultRepository) {
        this.requirementRepository = requirementRepository;
        this.bidRepository = bidRepository;
        this.matchResultRepository = matchResultRepository;
    }

    @Transactional
    public RequirementCreateResponse register(Long bidId, RequirementCreateRequest request) {
        requireBid(bidId);

        List<RequirementItem> saved = requirementRepository.saveAll(
                request.items().stream().map(item -> item.toEntity(bidId)).toList());

        // 요구사항이 바뀌면 기존 갭분석표는 더 이상 맞지 않는다. 지우고 다시 매칭하게 한다.
        matchResultRepository.deleteByBidId(bidId);

        return new RequirementCreateResponse(bidId, saved.stream().map(RequirementItem::getId).toList());
    }

    public List<RequirementItemResponse> findByBid(Long bidId) {
        requireBid(bidId);
        return requirementRepository.findByBidIdOrderByIdAsc(bidId).stream()
                .map(RequirementItemResponse::from)
                .toList();
    }

    @Transactional
    public void delete(Long bidId, Long requirementId) {
        RequirementItem item = requirementRepository.findById(requirementId)
                .orElseThrow(() -> NotFoundException.requirement(requirementId));
        if (!item.getBidId().equals(bidId)) {
            throw NotFoundException.requirement(requirementId);
        }
        matchResultRepository.deleteByRequirementIdIn(List.of(requirementId));
        requirementRepository.delete(item);
    }

    private void requireBid(Long bidId) {
        if (!bidRepository.existsById(bidId)) {
            throw NotFoundException.bid(bidId);
        }
    }
}
