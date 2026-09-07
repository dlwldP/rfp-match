package com.rfpmatch.requirement.dto;

import java.util.List;

/** 등록 결과. 프런트가 곧바로 상세를 다시 부르지 않아도 되도록 생성된 ID를 돌려준다. */
public record RequirementCreateResponse(Long bidId, List<Long> requirementIds) {
}
