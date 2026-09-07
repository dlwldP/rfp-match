package com.rfpmatch.match.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("단위 정규화")
class UnitNormalizerTest {

    @Nested
    @DisplayName("값과 단위를 기준 단위로 환산한다")
    class Normalize {

        @Test
        @DisplayName("단위가 별도 컬럼으로 들어온 경우")
        void separateUnit() {
            Optional<Quantity> quantity = UnitNormalizer.normalize("10", "Gbps");

            assertThat(quantity).isPresent();
            assertThat(quantity.get().dimension()).isEqualTo(Dimension.THROUGHPUT);
            assertThat(quantity.get().baseValue()).isEqualByComparingTo(new BigDecimal("10000000000"));
        }

        @Test
        @DisplayName("값 안에 단위가 붙어 있는 경우")
        void inlineUnit() {
            Optional<Quantity> quantity = UnitNormalizer.normalize("20Gbps", null);

            assertThat(quantity).isPresent();
            assertThat(quantity.get().baseValue()).isEqualByComparingTo(new BigDecimal("20000000000"));
            assertThat(quantity.get().displayText()).isEqualTo("20Gbps");
        }

        @Test
        @DisplayName("Mbps와 Gbps는 같은 계열이라 비교할 수 있다")
        void sameDimension() {
            Quantity mbps = UnitNormalizer.normalize("5000", "Mbps").orElseThrow();
            Quantity gbps = UnitNormalizer.normalize("10", "Gbps").orElseThrow();

            assertThat(mbps.comparableWith(gbps)).isTrue();
            assertThat(mbps.compareTo(gbps)).isNegative();
        }

        @Test
        @DisplayName("한글 자릿수 표기(만/억)를 숫자로 환산한다")
        void koreanMultiplier() {
            Quantity quantity = UnitNormalizer.normalize("1000만", "세션").orElseThrow();

            assertThat(quantity.baseValue()).isEqualByComparingTo(new BigDecimal("10000000"));
            assertThat(quantity.dimension()).isEqualTo(Dimension.COUNT);
        }

        @Test
        @DisplayName("천 단위 쉼표가 있어도 읽는다")
        void withThousandSeparator() {
            Quantity quantity = UnitNormalizer.normalize("1,000,000", null).orElseThrow();

            assertThat(quantity.baseValue()).isEqualByComparingTo(new BigDecimal("1000000"));
        }

        @Test
        @DisplayName("전송속도와 개수는 서로 다른 계열이라 비교 대상이 아니다")
        void differentDimension() {
            Quantity throughput = UnitNormalizer.normalize("10", "Gbps").orElseThrow();
            Quantity sessions = UnitNormalizer.normalize("10000", "세션").orElseThrow();

            assertThat(throughput.comparableWith(sessions)).isFalse();
        }

        @Test
        @DisplayName("수치로 읽을 수 없는 값은 비어 있는 결과를 준다")
        void notNumeric() {
            assertThat(UnitNormalizer.normalize("EAL4", null)).isEmpty();
            assertThat(UnitNormalizer.normalize("", "Gbps")).isEmpty();
            assertThat(UnitNormalizer.normalize(null, null)).isEmpty();
        }

        @Test
        @DisplayName("등록되지 않은 단위는 비교하지 않는다")
        void unknownUnit() {
            assertThat(UnitNormalizer.normalize("3", "년")).isEmpty();
        }
    }
}
