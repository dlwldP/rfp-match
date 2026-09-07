package com.rfpmatch.match.engine;

import com.rfpmatch.product.domain.ProductSpecDetail;
import com.rfpmatch.requirement.domain.RequirementItem;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Boolean형(국정원 보안적합성 검증필 여부, 이중화 지원 여부 등) 판정.
 * 요구가 "필요 없음"인 경우에는 제품 값과 무관하게 충족으로 본다.
 */
@Component
@Order(20)
public class BooleanMatchRule implements MatchRule {

    @Override
    public boolean supports(RequirementItem requirement, ProductSpecDetail spec) {
        return BooleanValue.parse(requirement.getRequiredValue()).isPresent()
                && BooleanValue.parse(spec.getSpecValue()).isPresent();
    }

    @Override
    public MatchEvaluation evaluate(RequirementItem requirement, ProductSpecDetail spec) {
        Optional<Boolean> required = BooleanValue.parse(requirement.getRequiredValue());
        Optional<Boolean> product = BooleanValue.parse(spec.getSpecValue());

        if (required.isEmpty() || product.isEmpty()) {
            return MatchEvaluation.unknown(spec.getSpecValue(), "예/아니오로 해석할 수 없는 값입니다.");
        }

        String productValue = BooleanValue.format(product.get());
        if (!required.get()) {
            return MatchEvaluation.satisfied(productValue, "요구되지 않는 항목");
        }
        return product.get()
                ? MatchEvaluation.satisfied(productValue, "요구 항목 보유")
                : MatchEvaluation.unsatisfied(productValue, "요구 항목 미보유 — 미충족");
    }
}
