package com.rfpmatch.common.config;

import com.rfpmatch.bid.domain.BidAnnouncement;
import com.rfpmatch.bid.repository.BidAnnouncementRepository;
import com.rfpmatch.product.domain.ProductSpec;
import com.rfpmatch.product.domain.ProductSpecDetail;
import com.rfpmatch.product.repository.ProductSpecRepository;
import com.rfpmatch.requirement.domain.RequirementItem;
import com.rfpmatch.requirement.repository.RequirementItemRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 인증키 없이도 화면과 매칭 결과를 확인할 수 있도록 예시 데이터를 넣는다.
 * 데이터가 이미 있으면 아무것도 하지 않는다. (app.sample-data.enabled=false 로 끌 수 있음)
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.sample-data.enabled", havingValue = "true")
public class SampleDataLoader {

    @Bean
    public ApplicationRunner sampleDataRunner(BidAnnouncementRepository bidRepository,
                                              RequirementItemRepository requirementRepository,
                                              ProductSpecRepository productRepository) {
        return args -> {
            if (bidRepository.count() > 0 || productRepository.count() > 0) {
                return;
            }
            LocalDateTime now = LocalDateTime.now();

            BidAnnouncement ips = bidRepository.save(BidAnnouncement.builder()
                    .bidNtceNo("20260900123")
                    .title("A기관 네트워크 침입방지시스템(IPS) 도입")
                    .institution("A기관")
                    .demandInstitution("A기관 정보보안부서")
                    .closeDate(now.plusDays(18).withHour(17).withMinute(0).withSecond(0).withNano(0))
                    .estimatedPrice(new BigDecimal("150000000"))
                    .syncedAt(now)
                    .build());

            BidAnnouncement ddos = bidRepository.save(BidAnnouncement.builder()
                    .bidNtceNo("20260900456")
                    .title("B기관 DDoS 대응장비 교체 구매")
                    .institution("B기관")
                    .demandInstitution("B기관 정보화부서")
                    .closeDate(now.plusDays(9).withHour(11).withMinute(0).withSecond(0).withNano(0))
                    .estimatedPrice(new BigDecimal("92000000"))
                    .syncedAt(now)
                    .build());

            requirementRepository.saveAll(List.of(
                    requirement(ips.getId(), "처리성능", "네트워크 처리량 10Gbps 이상", "10", "Gbps", true),
                    requirement(ips.getId(), "처리성능", "동시세션 1,000만 이상 지원", "1000만", null, true),
                    requirement(ips.getId(), "인증", "CC인증 EAL2 이상 필수", "EAL2", null, true),
                    requirement(ips.getId(), "인증", "국가정보원 보안적합성 검증필 제품", "YES", null, true),
                    requirement(ips.getId(), "기능", "장비 이중화(HA) 구성 지원", "YES", null, false),
                    requirement(ips.getId(), "유지보수", "무상 유지보수 3년 이상", "3", "년", false),
                    requirement(ddos.getId(), "처리성능", "방어 대역폭 20Gbps 이상", "20", "Gbps", true),
                    requirement(ddos.getId(), "인증", "국가정보원 보안적합성 검증필 제품", "YES", null, true)));

            ProductSpec ipsProduct = ProductSpec.builder().productName("NGIPS-2000").category("IPS").build();
            ipsProduct.addSpec(spec("처리성능", "20", "Gbps"));
            ipsProduct.addSpec(spec("동시세션", "2000만", null));
            ipsProduct.addSpec(spec("CC인증등급", "EAL4", null));
            ipsProduct.addSpec(spec("국정원검증필", "YES", null));
            ipsProduct.addSpec(spec("이중화", "YES", null));
            productRepository.save(ipsProduct);

            ProductSpec ddosProduct = ProductSpec.builder().productName("ADP-1000").category("DDoS").build();
            ddosProduct.addSpec(spec("처리성능", "10", "Gbps"));
            ddosProduct.addSpec(spec("CC인증등급", "EAL2", null));
            ddosProduct.addSpec(spec("국정원검증필", "YES", null));
            productRepository.save(ddosProduct);

            log.info("예시 데이터 적재 완료 (공고 2건, 요구사항 8건, 제품 2건)");
        };
    }

    private RequirementItem requirement(Long bidId, String category, String description,
                                        String requiredValue, String unit, boolean mandatory) {
        return RequirementItem.builder()
                .bidId(bidId)
                .category(category)
                .description(description)
                .requiredValue(requiredValue)
                .unit(unit)
                .mandatory(mandatory)
                .build();
    }

    private ProductSpecDetail spec(String key, String value, String unit) {
        return ProductSpecDetail.builder().specKey(key).specValue(value).unit(unit).build();
    }
}
