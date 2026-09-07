package com.rfpmatch.match.engine;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CC인증(공통평가기준) 보증등급. EAL1 &lt; EAL2 &lt; ... &lt; EAL7 순서이며,
 * 증강 표기(EAL4+)는 같은 숫자 등급보다 한 단계 위로 본다.
 *
 * @param level     EAL 숫자 등급 (1~7)
 * @param augmented 증강(+) 여부
 */
public record AssuranceLevel(int level, boolean augmented) implements Comparable<AssuranceLevel> {

    private static final Pattern EAL_PATTERN = Pattern.compile("eal\\s*([1-7])\\s*(\\+|증강)?");

    /** "EAL2", "EAL4+", "CC인증 EAL2 이상 필수" 같은 문구에서 등급을 뽑아낸다. */
    public static Optional<AssuranceLevel> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = EAL_PATTERN.matcher(raw.toLowerCase(Locale.KOREA));
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(new AssuranceLevel(
                Integer.parseInt(matcher.group(1)),
                matcher.group(2) != null));
    }

    /** 등급 비교용 점수. EAL4 < EAL4+ < EAL5 가 되도록 10단위로 벌려 둔다. */
    private int score() {
        return level * 10 + (augmented ? 5 : 0);
    }

    @Override
    public int compareTo(AssuranceLevel other) {
        return Integer.compare(this.score(), other.score());
    }

    /** 한 단계(EAL n → n+1) 차이인지. 부분충족 판정에 쓴다. */
    public boolean isOneStepBelow(AssuranceLevel required) {
        return required.level - this.level == 1;
    }

    @Override
    public String toString() {
        return "EAL" + level + (augmented ? "+" : "");
    }
}
