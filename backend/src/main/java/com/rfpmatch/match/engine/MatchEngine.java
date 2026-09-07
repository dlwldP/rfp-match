package com.rfpmatch.match.engine;

import com.rfpmatch.product.domain.ProductSpec;
import com.rfpmatch.product.domain.ProductSpecDetail;
import com.rfpmatch.requirement.domain.RequirementItem;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 갭분석의 핵심. 요구사항 한 줄마다 비교할 제품 스펙을 찾고, 값의 유형에 맞는 규칙으로 판정한다.
 * 비교할 스펙 자체가 없으면 "미충족"이 아니라 "확인불가"로 둔다 — 카탈로그에 안 적혀 있을 뿐
 * 실제로는 지원하는 기능일 수 있고, 이 판단은 담당자 몫이기 때문이다.
 */
@Component
public class MatchEngine {

    private final SpecKeyResolver specKeyResolver;
    private final List<MatchRule> rules;

    /** rules 는 {@code @Order} 순으로 주입된다 (등급 → Boolean → 수치 → 텍스트). */
    public MatchEngine(SpecKeyResolver specKeyResolver, List<MatchRule> rules) {
        this.specKeyResolver = specKeyResolver;
        this.rules = rules;
    }

    public MatchEvaluation evaluate(RequirementItem requirement, ProductSpec product) {
        List<ProductSpecDetail> specs = product.getSpecs();
        Optional<ProductSpecDetail> target = specKeyResolver.resolve(requirement, specs);

        if (target.isEmpty()) {
            return MatchEvaluation.unknown(null,
                    "제품 스펙에 대응 항목이 없습니다 — 스펙 등록 또는 담당자 확인 필요");
        }

        ProductSpecDetail spec = target.get();
        return rules.stream()
                .filter(rule -> rule.supports(requirement, spec))
                .findFirst()
                .map(rule -> rule.evaluate(requirement, spec))
                .orElseGet(() -> MatchEvaluation.unknown(spec.getSpecValue(), "적용 가능한 판정 규칙이 없습니다."));
    }
}
