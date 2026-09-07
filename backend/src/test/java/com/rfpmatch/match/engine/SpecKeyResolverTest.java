package com.rfpmatch.match.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.rfpmatch.product.domain.ProductSpecDetail;
import com.rfpmatch.requirement.domain.RequirementItem;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("요구사항 ↔ 제품 스펙 항목 매핑")
class SpecKeyResolverTest {

    private final SpecKeyResolver resolver = new SpecKeyResolver();

    @Test
    @DisplayName("표현이 달라도 같은 항목이면 찾아낸다 (네트워크 처리량 ↔ 처리성능)")
    void resolveBySynonym() {
        Optional<ProductSpecDetail> resolved = resolver.resolve(
                requirement("처리성능", "네트워크 처리량 10Gbps 이상"),
                List.of(spec("처리성능", "20"), spec("CC인증등급", "EAL4")));

        assertThat(resolved).isPresent();
        assertThat(resolved.get().getSpecKey()).isEqualTo("처리성능");
    }

    @Test
    @DisplayName("같은 분류('인증')에 성격이 다른 항목이 섞여 있어도 원문 문구로 구분한다")
    void distinguishWithinSameCategory() {
        List<ProductSpecDetail> specs = List.of(spec("CC인증등급", "EAL4"), spec("국정원검증필", "YES"));

        assertThat(resolver.resolve(requirement("인증", "CC인증 EAL2 이상 필수"), specs))
                .get()
                .extracting(ProductSpecDetail::getSpecKey)
                .isEqualTo("CC인증등급");
        assertThat(resolver.resolve(requirement("인증", "국가정보원 보안적합성 검증필 제품"), specs))
                .get()
                .extracting(ProductSpecDetail::getSpecKey)
                .isEqualTo("국정원검증필");
    }

    @Test
    @DisplayName("분류가 뭉뚱그려져 있어도 문구가 더 구체적이면 문구를 따른다")
    void descriptionWinsOverCategory() {
        Optional<ProductSpecDetail> resolved = resolver.resolve(
                requirement("처리성능", "동시세션 1,000만 이상 지원"),
                List.of(spec("처리성능", "20"), spec("동시세션", "2000만")));

        assertThat(resolved).get().extracting(ProductSpecDetail::getSpecKey).isEqualTo("동시세션");
    }

    @Test
    @DisplayName("사전에 없는 항목명은 문구에 그대로 등장하는지로 찾는다")
    void resolveByLiteralKey() {
        Optional<ProductSpecDetail> resolved = resolver.resolve(
                requirement("기능", "SSL 가시성 기능 제공 여부"),
                List.of(spec("SSL 가시성", "지원")));

        assertThat(resolved).get().extracting(ProductSpecDetail::getSpecKey).isEqualTo("SSL 가시성");
    }

    @Test
    @DisplayName("대응하는 스펙이 없으면 비어 있는 결과를 준다")
    void notResolved() {
        Optional<ProductSpecDetail> resolved = resolver.resolve(
                requirement("유지보수", "무상 유지보수 3년 이상"),
                List.of(spec("처리성능", "20")));

        assertThat(resolved).isEmpty();
    }

    private RequirementItem requirement(String category, String description) {
        return RequirementItem.builder()
                .bidId(1L)
                .category(category)
                .description(description)
                .requiredValue("1")
                .mandatory(true)
                .build();
    }

    private ProductSpecDetail spec(String key, String value) {
        return ProductSpecDetail.builder().specKey(key).specValue(value).build();
    }
}
