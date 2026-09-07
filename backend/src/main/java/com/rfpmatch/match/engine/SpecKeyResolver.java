package com.rfpmatch.match.engine;

import com.rfpmatch.product.domain.ProductSpecDetail;
import com.rfpmatch.requirement.domain.RequirementItem;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * RFP 문구와 제품 카탈로그의 항목명은 표현이 제각각이다
 * ("네트워크 처리량" ↔ "처리성능", "CC인증 EAL2 이상" ↔ "CC인증등급").
 * 양쪽을 같은 표준 키로 환산해 비교 대상 스펙을 찾아 준다.
 */
@Component
public class SpecKeyResolver {

    /**
     * @param canonicalKey 표준 키
     * @param keywords     이 키로 볼 수 있는 표현들 (정규화된 형태로 비교)
     */
    private record Alias(String canonicalKey, List<String> keywords) {
    }

    private static final List<Alias> ALIASES = List.of(
            new Alias("THROUGHPUT", List.of("처리성능", "네트워크처리량", "처리량", "throughput", "대역폭", "bandwidth", "성능")),
            new Alias("CC_GRADE", List.of("cc인증등급", "cc인증", "국제공통평가기준", "공통평가기준", "eal", "cc")),
            new Alias("NIS_VERIFIED", List.of("국정원검증필", "보안적합성검증", "보안적합성", "국가정보원", "국정원", "검증필")),
            new Alias("CONCURRENT_SESSION", List.of("동시세션", "최대세션", "세션수", "concurrentsession", "세션")),
            new Alias("NEW_SESSION_RATE", List.of("신규세션", "초당세션", "연결처리", "cps")),
            new Alias("PACKET_RATE", List.of("패킷처리", "패킷처리율", "pps")),
            new Alias("LATENCY", List.of("지연시간", "지연", "latency")),
            new Alias("HA", List.of("이중화", "고가용성", "ha", "fail-over", "페일오버")),
            new Alias("PORT", List.of("인터페이스", "포트수", "포트", "port")),
            new Alias("GS_CERT", List.of("gs인증", "굿소프트웨어")),
            new Alias("WARRANTY", List.of("무상유지보수", "유지보수", "하자보수", "warranty")),
            new Alias("DELIVERY", List.of("납품기한", "납기", "delivery")));

    /**
     * 요구사항과 비교할 제품 스펙 한 줄을 찾는다.
     * 1) 표준 키가 같은 스펙 → 2) 항목명이 요구사항 문구에 그대로 등장하는 스펙 순으로 본다.
     */
    public Optional<ProductSpecDetail> resolve(RequirementItem requirement, List<ProductSpecDetail> specs) {
        String requirementKey = canonicalKeyOf(requirement);

        Optional<ProductSpecDetail> byCanonicalKey = specs.stream()
                .filter(spec -> canonicalKeyOf(spec.getSpecKey()).equals(requirementKey))
                .findFirst();
        if (byCanonicalKey.isPresent()) {
            return byCanonicalKey;
        }

        String haystack = normalize(requirement.getCategory() + requirement.getDescription());
        return specs.stream()
                .filter(spec -> {
                    String key = normalize(spec.getSpecKey());
                    return !key.isBlank() && haystack.contains(key);
                })
                // 문구에 걸린 항목명이 여럿이면 더 긴(=더 구체적인) 쪽을 고른다.
                .max(Comparator.comparingInt(spec -> normalize(spec.getSpecKey()).length()));
    }

    /**
     * 요구사항의 표준 키.
     * 분류("처리성능")보다 원문 문구("동시세션 1,000만 이상")가 구체적이므로 문구를 먼저 본다.
     * 같은 분류 아래 성격이 다른 항목이 여러 줄 들어오는 일이 흔해서 분류만으로는 스펙을 특정할 수 없다.
     */
    public String canonicalKeyOf(RequirementItem requirement) {
        return matchAlias(normalize(requirement.getDescription()))
                .or(() -> matchAlias(normalize(requirement.getCategory())))
                .orElseGet(() -> normalize(requirement.getCategory()));
    }

    /** 제품 스펙 항목명의 표준 키. */
    public String canonicalKeyOf(String specKey) {
        String normalized = normalize(specKey);
        return matchAlias(normalized).orElse(normalized);
    }

    /** 문구 안에서 가장 구체적으로(=가장 길게) 걸리는 별칭을 고른다. */
    private Optional<String> matchAlias(String normalizedText) {
        if (normalizedText.isBlank()) {
            return Optional.empty();
        }
        String best = null;
        int bestLength = 0;
        for (Alias alias : ALIASES) {
            for (String keyword : alias.keywords()) {
                if (normalizedText.contains(keyword) && keyword.length() > bestLength) {
                    best = alias.canonicalKey();
                    bestLength = keyword.length();
                }
            }
        }
        return Optional.ofNullable(best);
    }

    /** 공백/기호/대소문자 차이를 제거해 비교 가능한 형태로 만든다. */
    private String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.toLowerCase(Locale.KOREA).replaceAll("[^0-9a-z가-힣]", "");
    }
}
