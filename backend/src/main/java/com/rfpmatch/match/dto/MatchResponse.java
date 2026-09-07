package com.rfpmatch.match.dto;

import com.rfpmatch.match.domain.MatchResult;
import com.rfpmatch.match.domain.MatchStatus;
import com.rfpmatch.requirement.domain.RequirementItem;
import java.time.LocalDateTime;
import java.util.List;

/** 갭분석표 한 장. 요구사항 줄별 판정 + 요약 통계. */
public record MatchResponse(
        Long bidId,
        String bidTitle,
        Long productId,
        String productName,
        LocalDateTime matchedAt,
        List<MatchItemResponse> results,
        MatchSummary summary) {

    /**
     * 갭분석표 한 줄.
     *
     * @param requiredValue RFP 요구값 (단위 포함 표기)
     * @param productValue  비교한 제품 스펙 값. 대응 스펙이 없으면 null
     * @param note          판정 근거
     */
    public record MatchItemResponse(
            Long requirementId,
            String category,
            String description,
            MatchStatus status,
            String requiredValue,
            String productValue,
            boolean mandatory,
            String note) {

        public static MatchItemResponse of(RequirementItem requirement, MatchResult result) {
            return new MatchItemResponse(
                    requirement.getId(),
                    requirement.getCategory(),
                    requirement.getDescription(),
                    result.getStatus(),
                    displayRequiredValue(requirement),
                    result.getMatchedValue(),
                    requirement.isMandatory(),
                    result.getNote());
        }

        private static String displayRequiredValue(RequirementItem requirement) {
            String unit = requirement.getUnit();
            return unit == null || unit.isBlank()
                    ? requirement.getRequiredValue()
                    : requirement.getRequiredValue() + unit;
        }
    }

    /**
     * 요약 통계.
     *
     * @param satisfactionRate      충족률(%). 부분충족은 0.5건으로 계산한다.
     * @param mandatoryUnsatisfied  필수 요구사항 중 미충족 건수
     * @param biddable              필수 요구사항에 미충족이 하나도 없으면 true (입찰 참여 가능 판단의 1차 신호)
     */
    public record MatchSummary(
            int totalCount,
            int satisfied,
            int partial,
            int unsatisfied,
            int unknown,
            double satisfactionRate,
            int mandatoryUnsatisfied,
            boolean biddable) {

        public static MatchSummary of(List<RequirementItem> requirements, List<MatchResult> results) {
            int total = results.size();
            int satisfied = count(results, MatchStatus.SATISFIED);
            int partial = count(results, MatchStatus.PARTIAL);
            int unsatisfied = count(results, MatchStatus.UNSATISFIED);
            int unknown = count(results, MatchStatus.UNKNOWN);

            List<Long> mandatoryIds = requirements.stream()
                    .filter(RequirementItem::isMandatory)
                    .map(RequirementItem::getId)
                    .toList();
            int mandatoryUnsatisfied = (int) results.stream()
                    .filter(result -> result.getStatus() == MatchStatus.UNSATISFIED)
                    .filter(result -> mandatoryIds.contains(result.getRequirementId()))
                    .count();

            double rate = total == 0 ? 0d
                    : Math.round((satisfied + partial * 0.5) / total * 1000) / 10d;

            return new MatchSummary(total, satisfied, partial, unsatisfied, unknown,
                    rate, mandatoryUnsatisfied, mandatoryUnsatisfied == 0);
        }

        private static int count(List<MatchResult> results, MatchStatus status) {
            return (int) results.stream().filter(result -> result.getStatus() == status).count();
        }
    }
}
