package com.rfpmatch.match.engine;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** "국정원 검증필 여부"처럼 예/아니오로 답하는 값의 표기 흔들림을 흡수한다. */
public final class BooleanValue {

    private static final List<String> TRUE_TOKENS = List.of(
            "yes", "y", "true", "o", "가능", "지원", "보유", "유", "있음", "충족", "검증필", "획득", "인증", "적용");

    private static final List<String> FALSE_TOKENS = List.of(
            "no", "n", "false", "x", "불가", "미지원", "미보유", "무", "없음", "미충족", "미검증", "미획득");

    private BooleanValue() {
    }

    public static Optional<Boolean> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String token = raw.trim().toLowerCase(Locale.KOREA);
        // "미지원"이 "지원"을 포함하므로 부정 토큰을 먼저 본다.
        if (FALSE_TOKENS.contains(token)) {
            return Optional.of(false);
        }
        if (TRUE_TOKENS.contains(token)) {
            return Optional.of(true);
        }
        return Optional.empty();
    }

    public static String format(boolean value) {
        return value ? "YES" : "NO";
    }
}
