package com.rfpmatch.match.domain;

import com.fasterxml.jackson.annotation.JsonValue;

/** 요구사항 한 줄에 대한 판정 결과. */
public enum MatchStatus {

    SATISFIED("충족"),
    PARTIAL("부분충족"),
    UNSATISFIED("미충족"),
    /** 자동 판정이 불가능해 담당자가 원문을 보고 최종 판단해야 하는 항목. */
    UNKNOWN("확인불가");

    private final String label;

    MatchStatus(String label) {
        this.label = label;
    }

    /** API 응답에는 영업 담당자가 그대로 읽을 수 있는 한글 라벨로 내려간다. */
    @JsonValue
    public String getLabel() {
        return label;
    }
}
