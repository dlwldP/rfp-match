package com.rfpmatch.match.engine;

import com.rfpmatch.product.domain.ProductSpecDetail;
import com.rfpmatch.requirement.domain.RequirementItem;

/**
 * 요구사항 유형별 판정 규칙. 값의 생김새로 담당 규칙이 정해지며,
 * {@link MatchEngine}이 등록 순서대로 물어보고 처음으로 supports 를 만족하는 규칙이 판정한다.
 * 새 유형(예: 날짜 비교)은 이 인터페이스 구현체를 추가하면 된다.
 */
public interface MatchRule {

    boolean supports(RequirementItem requirement, ProductSpecDetail spec);

    MatchEvaluation evaluate(RequirementItem requirement, ProductSpecDetail spec);
}
