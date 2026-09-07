package com.rfpmatch.match.engine;

import com.rfpmatch.product.domain.ProductSpecDetail;
import com.rfpmatch.requirement.domain.RequirementItem;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 수치형(처리성능, 세션 수, 지연시간 등) 판정.
 * 단위를 기준 단위로 환산한 뒤 부등호로 비교하며, RFP 문구의 "이상/이하"를 읽어 비교 방향을 정한다.
 * 요구치의 {@value #PARTIAL_TOLERANCE_PERCENT}% 이내로 모자란 값은 "부분충족"으로 남겨
 * 담당자가 상위 모델 제안이나 스펙 조정 협의를 판단하게 한다.
 */
@Component
@Order(30)
public class NumericMatchRule implements MatchRule {

    /** 부분충족으로 인정하는 허용 오차(%) */
    static final int PARTIAL_TOLERANCE_PERCENT = 10;

    private static final BigDecimal PARTIAL_RATIO =
            BigDecimal.valueOf(100 - PARTIAL_TOLERANCE_PERCENT).divide(BigDecimal.valueOf(100), MathContext.DECIMAL64);

    /** 값이 작을수록 좋은 요구사항("이하", "이내", "미만")인지 판별한다. */
    private static boolean isLowerBetter(RequirementItem requirement) {
        String text = requirement.getDescription() == null ? "" : requirement.getDescription();
        return text.contains("이하") || text.contains("이내") || text.contains("미만");
    }

    @Override
    public boolean supports(RequirementItem requirement, ProductSpecDetail spec) {
        return quantityOf(requirement).isPresent() && quantityOf(spec).isPresent();
    }

    @Override
    public MatchEvaluation evaluate(RequirementItem requirement, ProductSpecDetail spec) {
        Optional<Quantity> required = quantityOf(requirement);
        Optional<Quantity> product = quantityOf(spec);
        if (required.isEmpty() || product.isEmpty()) {
            return MatchEvaluation.unknown(spec.displayValue(), "수치로 해석할 수 없는 값입니다.");
        }

        Quantity req = required.get();
        Quantity prd = product.get();
        if (!prd.comparableWith(req)) {
            return MatchEvaluation.unknown(prd.displayText(),
                    "단위 계열이 달라 자동 비교 불가 (요구 %s / 제품 %s)".formatted(req.displayText(), prd.displayText()));
        }

        boolean lowerBetter = isLowerBetter(requirement);
        String comparison = "%s / 요구 %s".formatted(prd.displayText(), req.displayText());

        if (lowerBetter) {
            if (prd.compareTo(req) <= 0) {
                return MatchEvaluation.satisfied(prd.displayText(), comparison + " 이하 충족");
            }
            BigDecimal tolerated = req.baseValue().divide(PARTIAL_RATIO, MathContext.DECIMAL64);
            if (prd.baseValue().compareTo(tolerated) <= 0) {
                return MatchEvaluation.partial(prd.displayText(),
                        comparison + " — 허용오차 %d%% 이내 초과, 담당자 확인 필요".formatted(PARTIAL_TOLERANCE_PERCENT));
            }
            return MatchEvaluation.unsatisfied(prd.displayText(), comparison + " 초과 — 미충족");
        }

        if (prd.compareTo(req) >= 0) {
            return MatchEvaluation.satisfied(prd.displayText(), comparison + " 이상 충족");
        }
        BigDecimal threshold = req.baseValue().multiply(PARTIAL_RATIO, MathContext.DECIMAL64);
        if (prd.baseValue().compareTo(threshold) >= 0) {
            return MatchEvaluation.partial(prd.displayText(),
                    comparison + " — 요구치의 %d%% 이내 부족, 상위 모델 검토 필요".formatted(PARTIAL_TOLERANCE_PERCENT));
        }
        return MatchEvaluation.unsatisfied(prd.displayText(), comparison + " 미달");
    }

    private Optional<Quantity> quantityOf(RequirementItem requirement) {
        return UnitNormalizer.normalize(requirement.getRequiredValue(), requirement.getUnit());
    }

    private Optional<Quantity> quantityOf(ProductSpecDetail spec) {
        return UnitNormalizer.normalize(spec.getSpecValue(), spec.getUnit());
    }
}
