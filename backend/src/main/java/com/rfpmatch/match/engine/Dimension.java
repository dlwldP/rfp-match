package com.rfpmatch.match.engine;

/**
 * 수치 비교가 가능한 단위 계열. 서로 다른 계열끼리는 비교하지 않는다.
 * (예: 10Gbps 요구사항을 1,000,000 세션 스펙과 비교할 수는 없다.)
 */
public enum Dimension {
    /** 전송속도. 기준 단위 bps */
    THROUGHPUT("bps"),
    /** 패킷 처리율. 기준 단위 pps */
    PACKET_RATE("pps"),
    /** 저장/메모리 용량. 기준 단위 byte */
    DATA_SIZE("byte"),
    /** 초당 처리 건수. 기준 단위 cps */
    RATE_PER_SEC("cps"),
    /** 시간. 기준 단위 초 */
    TIME("s"),
    /** 백분율 */
    PERCENT("%"),
    /** 단위가 없거나 개수 계열(세션 수, 포트 수 등) */
    COUNT("");

    private final String baseUnit;

    Dimension(String baseUnit) {
        this.baseUnit = baseUnit;
    }

    public String getBaseUnit() {
        return baseUnit;
    }
}
