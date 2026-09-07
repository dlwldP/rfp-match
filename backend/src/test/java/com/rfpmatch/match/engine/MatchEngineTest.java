package com.rfpmatch.match.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.rfpmatch.match.domain.MatchStatus;
import com.rfpmatch.product.domain.ProductSpec;
import com.rfpmatch.product.domain.ProductSpecDetail;
import com.rfpmatch.requirement.domain.RequirementItem;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("매칭 엔진")
class MatchEngineTest {

    /** 실제 스프링이 @Order 로 주입하는 것과 같은 순서로 규칙을 구성한다. */
    private final MatchEngine engine = new MatchEngine(
            new SpecKeyResolver(),
            List.of(new GradeMatchRule(), new BooleanMatchRule(), new NumericMatchRule(), new TextMatchRule()));

    @Nested
    @DisplayName("수치형 요구사항")
    class Numeric {

        @Test
        @DisplayName("제품 성능이 요구치 이상이면 충족")
        void satisfied() {
            MatchEvaluation evaluation = engine.evaluate(
                    requirement("처리성능", "네트워크 처리량 10Gbps 이상", "10", "Gbps"),
                    product(spec("처리성능", "20", "Gbps")));

            assertThat(evaluation.status()).isEqualTo(MatchStatus.SATISFIED);
            assertThat(evaluation.matchedValue()).isEqualTo("20Gbps");
        }

        @Test
        @DisplayName("단위가 달라도 정규화 후 비교한다 (8000Mbps < 10Gbps → 미달)")
        void differentUnits() {
            MatchEvaluation evaluation = engine.evaluate(
                    requirement("처리성능", "네트워크 처리량 10Gbps 이상", "10", "Gbps"),
                    product(spec("처리성능", "8000", "Mbps")));

            assertThat(evaluation.status()).isEqualTo(MatchStatus.UNSATISFIED);
        }

        @Test
        @DisplayName("허용오차(10%) 이내로 모자라면 부분충족")
        void partial() {
            MatchEvaluation evaluation = engine.evaluate(
                    requirement("처리성능", "네트워크 처리량 10Gbps 이상", "10", "Gbps"),
                    product(spec("처리성능", "9.5", "Gbps")));

            assertThat(evaluation.status()).isEqualTo(MatchStatus.PARTIAL);
        }

        @Test
        @DisplayName("'이하' 요구사항은 값이 작을수록 충족이다")
        void lowerIsBetter() {
            MatchEvaluation satisfied = engine.evaluate(
                    requirement("성능", "지연시간 100ms 이하", "100", "ms"),
                    product(spec("지연시간", "40", "ms")));
            MatchEvaluation unsatisfied = engine.evaluate(
                    requirement("성능", "지연시간 100ms 이하", "100", "ms"),
                    product(spec("지연시간", "300", "ms")));

            assertThat(satisfied.status()).isEqualTo(MatchStatus.SATISFIED);
            assertThat(unsatisfied.status()).isEqualTo(MatchStatus.UNSATISFIED);
        }

        @Test
        @DisplayName("한글 자릿수 표기(1000만 세션)도 비교한다")
        void koreanNumber() {
            MatchEvaluation evaluation = engine.evaluate(
                    requirement("처리성능", "동시세션 1,000만 이상 지원", "1000만", null),
                    product(spec("동시세션", "2000만", null)));

            assertThat(evaluation.status()).isEqualTo(MatchStatus.SATISFIED);
        }
    }

    @Nested
    @DisplayName("등급형 요구사항")
    class Grade {

        @Test
        @DisplayName("요구 등급 이상이면 충족")
        void satisfied() {
            MatchEvaluation evaluation = engine.evaluate(
                    requirement("인증", "CC인증 EAL2 이상 필수", "EAL2", null),
                    product(spec("CC인증등급", "EAL4", null)));

            assertThat(evaluation.status()).isEqualTo(MatchStatus.SATISFIED);
            assertThat(evaluation.matchedValue()).isEqualTo("EAL4");
        }

        @Test
        @DisplayName("한 등급 부족하면 부분충족으로 담당자에게 넘긴다")
        void oneStepBelow() {
            MatchEvaluation evaluation = engine.evaluate(
                    requirement("인증", "CC인증 EAL4 이상 필수", "EAL4", null),
                    product(spec("CC인증등급", "EAL3", null)));

            assertThat(evaluation.status()).isEqualTo(MatchStatus.PARTIAL);
        }

        @Test
        @DisplayName("두 등급 이상 부족하면 미충족")
        void unsatisfied() {
            MatchEvaluation evaluation = engine.evaluate(
                    requirement("인증", "CC인증 EAL4 이상 필수", "EAL4", null),
                    product(spec("CC인증등급", "EAL2", null)));

            assertThat(evaluation.status()).isEqualTo(MatchStatus.UNSATISFIED);
        }
    }

    @Nested
    @DisplayName("Boolean형 요구사항")
    class Bool {

        @Test
        @DisplayName("국정원 검증필 보유 시 충족")
        void satisfied() {
            MatchEvaluation evaluation = engine.evaluate(
                    requirement("인증", "국가정보원 보안적합성 검증필 제품", "YES", null),
                    product(spec("국정원검증필", "YES", null)));

            assertThat(evaluation.status()).isEqualTo(MatchStatus.SATISFIED);
        }

        @Test
        @DisplayName("미보유 시 미충족")
        void unsatisfied() {
            MatchEvaluation evaluation = engine.evaluate(
                    requirement("인증", "국가정보원 보안적합성 검증필 제품", "YES", null),
                    product(spec("국정원검증필", "없음", null)));

            assertThat(evaluation.status()).isEqualTo(MatchStatus.UNSATISFIED);
            assertThat(evaluation.matchedValue()).isEqualTo("NO");
        }
    }

    @Nested
    @DisplayName("자동 판정이 어려운 요구사항")
    class Unknown {

        @Test
        @DisplayName("대응 스펙이 아예 없으면 미충족이 아니라 확인불가로 남긴다")
        void noMatchingSpec() {
            MatchEvaluation evaluation = engine.evaluate(
                    requirement("유지보수", "무상 유지보수 3년 이상", "3", "년"),
                    product(spec("처리성능", "20", "Gbps")));

            assertThat(evaluation.status()).isEqualTo(MatchStatus.UNKNOWN);
            assertThat(evaluation.matchedValue()).isNull();
            assertThat(evaluation.note()).contains("대응 항목이 없습니다");
        }

        @Test
        @DisplayName("텍스트형은 값이 같을 때만 충족, 그 외는 사람에게 넘긴다")
        void textRule() {
            MatchEvaluation same = engine.evaluate(
                    requirement("기능", "관리 인터페이스 방식", "웹 콘솔", null),
                    product(spec("관리 인터페이스", "웹 콘솔", null)));
            MatchEvaluation different = engine.evaluate(
                    requirement("기능", "관리 인터페이스 방식", "전용 CLI 도구", null),
                    product(spec("관리 인터페이스", "웹 콘솔", null)));

            assertThat(same.status()).isEqualTo(MatchStatus.SATISFIED);
            assertThat(different.status()).isEqualTo(MatchStatus.UNKNOWN);
        }
    }

    private RequirementItem requirement(String category, String description, String requiredValue, String unit) {
        return RequirementItem.builder()
                .bidId(1L)
                .category(category)
                .description(description)
                .requiredValue(requiredValue)
                .unit(unit)
                .mandatory(true)
                .build();
    }

    private ProductSpec product(ProductSpecDetail... details) {
        ProductSpec product = ProductSpec.builder().productName("테스트 제품").category("IPS").build();
        for (ProductSpecDetail detail : details) {
            product.addSpec(detail);
        }
        return product;
    }

    private ProductSpecDetail spec(String key, String value, String unit) {
        return ProductSpecDetail.builder().specKey(key).specValue(value).unit(unit).build();
    }
}
