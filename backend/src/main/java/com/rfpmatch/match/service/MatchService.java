package com.rfpmatch.match.service;

import com.rfpmatch.bid.domain.BidAnnouncement;
import com.rfpmatch.bid.service.BidService;
import com.rfpmatch.common.exception.NotFoundException;
import com.rfpmatch.match.domain.MatchResult;
import com.rfpmatch.match.dto.MatchResponse;
import com.rfpmatch.match.engine.MatchEngine;
import com.rfpmatch.match.engine.MatchEvaluation;
import com.rfpmatch.match.repository.MatchResultRepository;
import com.rfpmatch.product.domain.ProductSpec;
import com.rfpmatch.product.service.ProductService;
import com.rfpmatch.requirement.domain.RequirementItem;
import com.rfpmatch.requirement.repository.RequirementItemRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 매칭 실행과 갭분석 결과 조회. */
@Service
@Transactional(readOnly = true)
public class MatchService {

    private final MatchEngine matchEngine;
    private final MatchResultRepository matchResultRepository;
    private final RequirementItemRepository requirementRepository;
    private final BidService bidService;
    private final ProductService productService;

    public MatchService(MatchEngine matchEngine,
                        MatchResultRepository matchResultRepository,
                        RequirementItemRepository requirementRepository,
                        BidService bidService,
                        ProductService productService) {
        this.matchEngine = matchEngine;
        this.matchResultRepository = matchResultRepository;
        this.requirementRepository = requirementRepository;
        this.bidService = bidService;
        this.productService = productService;
    }

    /**
     * 공고의 요구사항 전체를 제품 스펙과 대조해 갭분석표를 만든다.
     * 같은 공고·제품 조합으로 다시 실행하면 이전 결과를 지우고 새로 쓴다(최신 판정만 유지).
     */
    @Transactional
    public MatchResponse match(Long bidId, Long productId) {
        BidAnnouncement bid = bidService.getBid(bidId);
        ProductSpec product = productService.getProduct(productId);

        List<RequirementItem> requirements = requirementRepository.findByBidIdOrderByIdAsc(bidId);
        if (requirements.isEmpty()) {
            throw new IllegalArgumentException(
                    "등록된 요구사항이 없어 매칭할 수 없습니다. 먼저 요구사항 항목을 등록하세요. bidId=" + bidId);
        }

        matchResultRepository.deleteByBidIdAndProductId(bidId, productId);

        LocalDateTime matchedAt = LocalDateTime.now();
        List<MatchResult> results = requirements.stream()
                .map(requirement -> toResult(bidId, product, requirement, matchedAt))
                .toList();
        List<MatchResult> saved = matchResultRepository.saveAll(results);

        return toResponse(bid, product, requirements, saved);
    }

    /** 저장된 갭분석 결과를 다시 조회한다. */
    public MatchResponse findResult(Long bidId, Long productId) {
        BidAnnouncement bid = bidService.getBid(bidId);
        ProductSpec product = productService.getProduct(productId);

        List<MatchResult> results = matchResultRepository.findByBidIdAndProductIdOrderByRequirementIdAsc(bidId, productId);
        if (results.isEmpty()) {
            throw new NotFoundException(
                    "매칭 결과가 없습니다. 먼저 매칭을 실행하세요. bidId=%d, productId=%d".formatted(bidId, productId));
        }
        return toResponse(bid, product, requirementRepository.findByBidIdOrderByIdAsc(bidId), results);
    }

    private MatchResult toResult(Long bidId, ProductSpec product, RequirementItem requirement,
                                 LocalDateTime matchedAt) {
        MatchEvaluation evaluation = matchEngine.evaluate(requirement, product);
        return MatchResult.builder()
                .bidId(bidId)
                .productId(product.getId())
                .requirementId(requirement.getId())
                .status(evaluation.status())
                .matchedValue(evaluation.matchedValue())
                .note(evaluation.note())
                .matchedAt(matchedAt)
                .build();
    }

    private MatchResponse toResponse(BidAnnouncement bid, ProductSpec product,
                                     List<RequirementItem> requirements, List<MatchResult> results) {
        Map<Long, RequirementItem> requirementsById = requirements.stream()
                .collect(Collectors.toMap(RequirementItem::getId, Function.identity()));

        List<MatchResponse.MatchItemResponse> items = results.stream()
                // 요구사항이 지워진 뒤 남은 결과 행은 표에서 제외한다.
                .filter(result -> requirementsById.containsKey(result.getRequirementId()))
                .map(result -> MatchResponse.MatchItemResponse.of(requirementsById.get(result.getRequirementId()), result))
                .toList();

        return new MatchResponse(
                bid.getId(),
                bid.getTitle(),
                product.getId(),
                product.getProductName(),
                results.isEmpty() ? null : results.get(0).getMatchedAt(),
                items,
                MatchResponse.MatchSummary.of(requirements, results));
    }
}
