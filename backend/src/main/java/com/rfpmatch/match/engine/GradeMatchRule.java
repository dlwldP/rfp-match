package com.rfpmatch.match.engine;

import com.rfpmatch.product.domain.ProductSpecDetail;
import com.rfpmatch.requirement.domain.RequirementItem;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 등급형(CC인증 EAL) 판정.
 * 요구등급 이상이면 충족, 딱 한 단계 아래면 "부분충족"으로 두어 담당자가 상위등급 제품 대체나
 * 인증 진행 상황을 확인하도록 남긴다.
 */
@Component
@Order(10)
public class GradeMatchRule implements MatchRule {

    @Override
    public boolean supports(RequirementItem requirement, ProductSpecDetail spec) {
        return AssuranceLevel.parse(requirement.getRequiredValue()).isPresent()
                || (AssuranceLevel.parse(requirement.getDescription()).isPresent()
                    && AssuranceLevel.parse(spec.getSpecValue()).isPresent());
    }

    @Override
    public MatchEvaluation evaluate(RequirementItem requirement, ProductSpecDetail spec) {
        Optional<AssuranceLevel> required = AssuranceLevel.parse(requirement.getRequiredValue())
                .or(() -> AssuranceLevel.parse(requirement.getDescription()));
        Optional<AssuranceLevel> product = AssuranceLevel.parse(spec.getSpecValue());

        if (required.isEmpty() || product.isEmpty()) {
            return MatchEvaluation.unknown(spec.getSpecValue(),
                    "CC인증 등급을 읽을 수 없습니다. 인증서 원본 확인 필요");
        }

        AssuranceLevel req = required.get();
        AssuranceLevel prd = product.get();
        String comparison = "%s / 요구 %s".formatted(prd, req);

        if (prd.compareTo(req) >= 0) {
            return MatchEvaluation.satisfied(prd.toString(), comparison + " 이상 충족");
        }
        if (prd.isOneStepBelow(req)) {
            return MatchEvaluation.partial(prd.toString(),
                    comparison + " — 한 등급 부족, 상위등급 모델 대체 검토 필요");
        }
        return MatchEvaluation.unsatisfied(prd.toString(), comparison + " 미달");
    }
}
