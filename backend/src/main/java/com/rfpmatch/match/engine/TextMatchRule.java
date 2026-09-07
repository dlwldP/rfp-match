package com.rfpmatch.match.engine;

import com.rfpmatch.product.domain.ProductSpecDetail;
import com.rfpmatch.requirement.domain.RequirementItem;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 텍스트형 요구사항의 마지막 판정 규칙.
 * MVP에서는 자동으로 "충족"을 선언하지 않는다. 값이 정확히 같을 때만 충족으로 보고,
 * 키워드가 겹치면 "부분충족", 그 외에는 "확인불가"로 남겨 담당자가 원문을 보고 판단하게 한다.
 * (잘못된 자동 충족 판정은 입찰 실격으로 이어지므로, 보수적으로 사람에게 넘긴다.)
 */
@Component
@Order(Integer.MAX_VALUE)
public class TextMatchRule implements MatchRule {

    /** 조사/수식어라 비교 의미가 없는 토큰 */
    private static final List<String> STOP_WORDS = List.of("이상", "이하", "이내", "필수", "제품", "지원", "필요", "가능", "여부");

    private static final int MIN_KEYWORD_LENGTH = 2;

    @Override
    public boolean supports(RequirementItem requirement, ProductSpecDetail spec) {
        return true; // 최후순위 fallback
    }

    @Override
    public MatchEvaluation evaluate(RequirementItem requirement, ProductSpecDetail spec) {
        String required = requirement.getRequiredValue().trim();
        String productValue = spec.getSpecValue().trim();

        if (required.equalsIgnoreCase(productValue)) {
            return MatchEvaluation.satisfied(productValue, "요구값과 동일");
        }

        List<String> keywords = keywordsOf(required);
        String haystack = productValue.toLowerCase(Locale.KOREA);
        long hits = keywords.stream().filter(haystack::contains).count();

        if (!keywords.isEmpty() && hits == keywords.size()) {
            return MatchEvaluation.partial(productValue, "요구 키워드가 스펙에 포함됨 — 담당자 최종 확인 필요");
        }
        if (hits > 0) {
            return MatchEvaluation.partial(productValue,
                    "요구 키워드 일부(%d/%d)만 일치 — 담당자 최종 확인 필요".formatted(hits, keywords.size()));
        }
        return MatchEvaluation.unknown(productValue, "자동 판정 불가 — RFP 원문과 대조해 담당자가 판단해야 하는 항목");
    }

    private List<String> keywordsOf(String text) {
        return Arrays.stream(text.toLowerCase(Locale.KOREA).split("[\\s,/()·]+"))
                .map(token -> token.replaceAll("[^0-9a-z가-힣]", ""))
                .filter(token -> token.length() >= MIN_KEYWORD_LENGTH)
                .filter(token -> !STOP_WORDS.contains(token))
                .toList();
    }
}
