package com.rfpmatch.match.engine;

import com.rfpmatch.match.domain.MatchStatus;

/**
 * 요구사항 한 줄에 대한 판정 결과.
 *
 * @param status       충족/부분충족/미충족/확인불가
 * @param matchedValue 비교에 쓴 제품 스펙 값 (없으면 null)
 * @param note         판정 근거. 갭분석표에 그대로 노출된다.
 */
public record MatchEvaluation(MatchStatus status, String matchedValue, String note) {

    public static MatchEvaluation satisfied(String matchedValue, String note) {
        return new MatchEvaluation(MatchStatus.SATISFIED, matchedValue, note);
    }

    public static MatchEvaluation partial(String matchedValue, String note) {
        return new MatchEvaluation(MatchStatus.PARTIAL, matchedValue, note);
    }

    public static MatchEvaluation unsatisfied(String matchedValue, String note) {
        return new MatchEvaluation(MatchStatus.UNSATISFIED, matchedValue, note);
    }

    public static MatchEvaluation unknown(String matchedValue, String note) {
        return new MatchEvaluation(MatchStatus.UNKNOWN, matchedValue, note);
    }
}
