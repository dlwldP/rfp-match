package com.rfpmatch.match.engine;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * "10Gbps", "10" + 단위 "Gbps", "1000만 세션" 같은 표기를 계열별 기준 단위 값으로 환산한다.
 * RFP 문구와 제품 카탈로그가 서로 다른 단위를 쓰는 일이 잦아 비교 전에 반드시 거쳐야 하는 단계다.
 */
public final class UnitNormalizer {

    /** 숫자(1,000 / 10.5 형태) + 한글 자릿수 접미사 + 단위 를 분리한다. */
    private static final Pattern VALUE_PATTERN = Pattern.compile(
            "^\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*([천만억조])?\\s*([a-zA-Z/%가-힣]*)\\s*$");

    private static final Map<String, BigDecimal> KOREAN_MULTIPLIERS = Map.of(
            "천", new BigDecimal("1000"),
            "만", new BigDecimal("10000"),
            "억", new BigDecimal("100000000"),
            "조", new BigDecimal("1000000000000"));

    private record UnitDef(Dimension dimension, BigDecimal factor) {
    }

    private static final Map<String, UnitDef> UNITS = new LinkedHashMap<>();

    static {
        // 전송속도 (bps 기준)
        throughput("bps", "1");
        throughput("bit/s", "1");
        throughput("kbps", "1000");
        throughput("kb/s", "1000");
        throughput("mbps", "1000000");
        throughput("mb/s", "1000000");
        throughput("gbps", "1000000000");
        throughput("gb/s", "1000000000");
        throughput("g", "1000000000"); // "10G" 같은 축약 표기
        throughput("tbps", "1000000000000");

        // 패킷 처리율 (pps 기준)
        unit(Dimension.PACKET_RATE, "pps", "1");
        unit(Dimension.PACKET_RATE, "kpps", "1000");
        unit(Dimension.PACKET_RATE, "mpps", "1000000");
        unit(Dimension.PACKET_RATE, "gpps", "1000000000");

        // 용량 (byte 기준, 1024 배수)
        unit(Dimension.DATA_SIZE, "byte", "1");
        unit(Dimension.DATA_SIZE, "b", "1");
        unit(Dimension.DATA_SIZE, "kb", "1024");
        unit(Dimension.DATA_SIZE, "mb", "1048576");
        unit(Dimension.DATA_SIZE, "gb", "1073741824");
        unit(Dimension.DATA_SIZE, "tb", "1099511627776");

        // 초당 처리 건수
        unit(Dimension.RATE_PER_SEC, "cps", "1");
        unit(Dimension.RATE_PER_SEC, "tps", "1");
        unit(Dimension.RATE_PER_SEC, "qps", "1");

        // 시간 (초 기준)
        unit(Dimension.TIME, "ms", "0.001");
        unit(Dimension.TIME, "s", "1");
        unit(Dimension.TIME, "sec", "1");
        unit(Dimension.TIME, "초", "1");
        unit(Dimension.TIME, "min", "60");
        unit(Dimension.TIME, "분", "60");
        unit(Dimension.TIME, "hour", "3600");
        unit(Dimension.TIME, "시간", "3600");

        unit(Dimension.PERCENT, "%", "1");

        // 개수 계열 (단위가 없거나 세션/포트 수 등)
        unit(Dimension.COUNT, "", "1");
        unit(Dimension.COUNT, "개", "1");
        unit(Dimension.COUNT, "ea", "1");
        unit(Dimension.COUNT, "건", "1");
        unit(Dimension.COUNT, "port", "1");
        unit(Dimension.COUNT, "포트", "1");
        unit(Dimension.COUNT, "session", "1");
        unit(Dimension.COUNT, "sessions", "1");
        unit(Dimension.COUNT, "세션", "1");
        unit(Dimension.COUNT, "user", "1");
        unit(Dimension.COUNT, "명", "1");
    }

    private static void throughput(String symbol, String factor) {
        unit(Dimension.THROUGHPUT, symbol, factor);
    }

    private static void unit(Dimension dimension, String symbol, String factor) {
        UNITS.put(symbol, new UnitDef(dimension, new BigDecimal(factor)));
    }

    private UnitNormalizer() {
    }

    /**
     * 값과 단위를 기준 단위로 환산한다. 단위가 값 안에 붙어 있어도("10Gbps") 되고,
     * 별도 컬럼으로 들어와도("10" + "Gbps") 된다. 숫자로 읽히지 않으면 비어 있는 결과를 돌려준다.
     */
    public static Optional<Quantity> normalize(String rawValue, String rawUnit) {
        if (rawValue == null || rawValue.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = VALUE_PATTERN.matcher(rawValue.trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }

        BigDecimal number = new BigDecimal(matcher.group(1).replace(",", ""));
        String koreanMultiplier = matcher.group(2);
        if (koreanMultiplier != null) {
            number = number.multiply(KOREAN_MULTIPLIERS.get(koreanMultiplier));
        }

        String inlineUnit = matcher.group(3);
        String unitSymbol = normalizeSymbol(!inlineUnit.isBlank() ? inlineUnit : rawUnit);

        UnitDef def = UNITS.get(unitSymbol);
        if (def == null) {
            return Optional.empty();
        }
        return Optional.of(new Quantity(
                number.multiply(def.factor()),
                def.dimension(),
                display(rawValue, rawUnit)));
    }

    private static String normalizeSymbol(String unit) {
        if (unit == null) {
            return "";
        }
        return unit.trim().toLowerCase(Locale.KOREA).replace(" ", "");
    }

    private static String display(String rawValue, String rawUnit) {
        String value = rawValue.trim();
        if (rawUnit == null || rawUnit.isBlank()) {
            return value;
        }
        // 값에 이미 단위가 붙어 있으면 중복 표기하지 않는다.
        return value.toLowerCase(Locale.KOREA).endsWith(rawUnit.trim().toLowerCase(Locale.KOREA))
                ? value
                : value + rawUnit.trim();
    }
}
