package com.rfpmatch.match.engine;

import java.math.BigDecimal;

/**
 * 기준 단위로 환산된 수치.
 *
 * @param baseValue   계열의 기준 단위로 환산한 값 (예: 10Gbps → 10,000,000,000)
 * @param dimension   단위 계열
 * @param displayText 사람이 읽는 원래 표기 (예: "10Gbps")
 */
public record Quantity(BigDecimal baseValue, Dimension dimension, String displayText) {

    public boolean comparableWith(Quantity other) {
        return this.dimension == other.dimension;
    }

    public int compareTo(Quantity other) {
        return this.baseValue.compareTo(other.baseValue);
    }
}
