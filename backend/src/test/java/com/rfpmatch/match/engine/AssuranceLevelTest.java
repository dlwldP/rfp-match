package com.rfpmatch.match.engine;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CC인증 등급")
class AssuranceLevelTest {

    @Test
    @DisplayName("문장 속에 섞여 있어도 등급을 뽑아낸다")
    void parseFromSentence() {
        assertThat(AssuranceLevel.parse("CC인증 EAL2 이상 필수")).contains(new AssuranceLevel(2, false));
        assertThat(AssuranceLevel.parse("eal 4")).contains(new AssuranceLevel(4, false));
        assertThat(AssuranceLevel.parse("EAL4+")).contains(new AssuranceLevel(4, true));
    }

    @Test
    @DisplayName("등급 표기가 없으면 비어 있는 결과를 준다")
    void parseNothing() {
        assertThat(AssuranceLevel.parse("국정원 검증필")).isEmpty();
        assertThat(AssuranceLevel.parse(null)).isEmpty();
    }

    @Test
    @DisplayName("EAL4 < EAL4+ < EAL5 순으로 정렬된다")
    void ordering() {
        AssuranceLevel eal4 = new AssuranceLevel(4, false);
        AssuranceLevel eal4Plus = new AssuranceLevel(4, true);
        AssuranceLevel eal5 = new AssuranceLevel(5, false);

        assertThat(eal4.compareTo(eal4Plus)).isNegative();
        assertThat(eal4Plus.compareTo(eal5)).isNegative();
        assertThat(eal5.compareTo(eal4)).isPositive();
    }

    @Test
    @DisplayName("한 등급 차이를 구분한다")
    void oneStepBelow() {
        assertThat(new AssuranceLevel(2, false).isOneStepBelow(new AssuranceLevel(3, false))).isTrue();
        assertThat(new AssuranceLevel(2, false).isOneStepBelow(new AssuranceLevel(4, false))).isFalse();
    }

    @Test
    @DisplayName("표기는 EAL 형식으로 되돌린다")
    void format() {
        assertThat(new AssuranceLevel(4, true)).hasToString("EAL4+");
        assertThat(new AssuranceLevel(2, false)).hasToString("EAL2");
    }
}
