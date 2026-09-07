package com.rfpmatch.mysql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rfpmatch.bid.domain.BidAnnouncement;
import com.rfpmatch.bid.dto.BidSummaryResponse;
import com.rfpmatch.bid.repository.BidAnnouncementRepository;
import com.rfpmatch.bid.service.BidService;
import com.rfpmatch.common.dto.PageResponse;
import com.rfpmatch.match.domain.MatchStatus;
import com.rfpmatch.match.dto.MatchResponse;
import com.rfpmatch.match.repository.MatchResultRepository;
import com.rfpmatch.match.service.MatchService;
import com.rfpmatch.product.domain.ProductSpec;
import com.rfpmatch.product.domain.ProductSpecDetail;
import com.rfpmatch.product.repository.ProductSpecRepository;
import com.rfpmatch.requirement.domain.RequirementItem;
import com.rfpmatch.requirement.repository.RequirementItemRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 실제 MySQL 8에서의 동작 검증.
 *
 * <p>단위/통합 테스트는 H2({@code MODE=MySQL})에서 돌지만 H2는 문법만 흉내 낼 뿐이라
 * 방언 차이(널 정렬, 문자셋, 제약조건)는 잡지 못한다. 이 클래스는 운영과 같은 MySQL 8 컨테이너를 띄워
 * 스키마 생성부터 매칭 결과 저장까지 한 번 훑는다.
 *
 * <p>실행에는 Docker 데몬이 필요하며, 기본 빌드에서는 제외된다:
 * <pre>
 * mvn test                      # H2 단위/통합 테스트만
 * mvn verify -Pmysql-test       # 위 + 이 클래스
 * </pre>
 */
@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
        // 운영과 같은 경로(ddl-auto=update)로 스키마가 만들어지는지까지 확인한다.
        "spring.jpa.hibernate.ddl-auto=update",
        "app.sample-data.enabled=false",
        "bid-api.enabled=false"
})
@DisplayName("MySQL 8 호환성")
class MySqlCompatibilityIT {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("rfpmatch")
            .withUrlParam("characterEncoding", "UTF-8")
            .withUrlParam("serverTimezone", "Asia/Seoul");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BidAnnouncementRepository bidRepository;

    @Autowired
    private RequirementItemRepository requirementRepository;

    @Autowired
    private ProductSpecRepository productRepository;

    @Autowired
    private MatchResultRepository matchResultRepository;

    @Autowired
    private BidService bidService;

    @Autowired
    private MatchService matchService;

    /** 트랜잭션 롤백에 기대지 않고 실제 커밋을 검증하므로 직접 정리한다. */
    @AfterEach
    void cleanUp() {
        matchResultRepository.deleteAll();
        requirementRepository.deleteAll();
        productRepository.deleteAll();
        bidRepository.deleteAll();
    }

    @Test
    @DisplayName("ddl-auto=update 로 5개 테이블이 생성된다")
    void schemaIsCreated() {
        List<String> tables = jdbcTemplate.queryForList("""
                select table_name from information_schema.tables where table_schema = database()
                """, String.class);

        assertThat(tables).contains(
                "bid_announcement", "requirement_item", "product_spec", "product_spec_detail", "match_result");
    }

    @Test
    @DisplayName("한글이 깨지지 않고 저장·조회된다 (utf8mb4)")
    void koreanTextRoundTrip() {
        String title = "A기관 네트워크 침입방지시스템(IPS) 도입 — 재공고";
        bidRepository.save(BidAnnouncement.builder()
                .bidNtceNo("20260900123")
                .title(title)
                .institution("A기관")
                .syncedAt(LocalDateTime.now())
                .build());

        assertThat(bidRepository.findByBidNtceNo("20260900123").orElseThrow().getTitle()).isEqualTo(title);
    }

    @Test
    @DisplayName("마감일이 없는 공고는 목록 맨 뒤로 밀린다 (nulls last 는 MySQL 에 없는 구문이라 방언 검증이 필요하다)")
    void nullCloseDateSortsLast() {
        LocalDateTime now = LocalDateTime.now();
        saveBid("20260900001", "마감 임박 공고", now.plusDays(3));
        saveBid("20260900002", "여유 있는 공고", now.plusDays(30));
        saveBid("20260900003", "마감일 미정 공고", null);

        PageResponse<BidSummaryResponse> page = bidService.search(null, null, PageRequest.of(0, 10));

        assertThat(page.content()).extracting(BidSummaryResponse::title)
                .containsExactly("여유 있는 공고", "마감 임박 공고", "마감일 미정 공고");
    }

    @Test
    @DisplayName("같은 공고번호는 유니크 제약으로 중복 저장되지 않는다")
    void duplicateBidNoIsRejected() {
        saveBid("20260900123", "최초 공고", LocalDateTime.now().plusDays(10));

        assertThatThrownBy(() -> bidRepository.saveAndFlush(BidAnnouncement.builder()
                .bidNtceNo("20260900123")
                .title("중복 공고")
                .syncedAt(LocalDateTime.now())
                .build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("매칭 결과가 저장되고 판정·근거가 그대로 다시 조회된다")
    void matchResultPersists() {
        Long bidId = saveBid("20260900123", "A기관 IPS 도입", LocalDateTime.now().plusDays(10)).getId();
        requirementRepository.saveAll(List.of(
                requirement(bidId, "처리성능", "네트워크 처리량 10Gbps 이상", "10", "Gbps", true),
                requirement(bidId, "인증", "CC인증 EAL2 이상 필수", "EAL2", null, true),
                requirement(bidId, "유지보수", "무상 유지보수 3년 이상", "3", "년", false)));
        Long productId = saveProduct();

        MatchResponse executed = matchService.match(bidId, productId);
        MatchResponse reloaded = matchService.findResult(bidId, productId);

        assertThat(executed.summary().totalCount()).isEqualTo(3);
        assertThat(reloaded.results()).extracting(MatchResponse.MatchItemResponse::status)
                .containsExactly(MatchStatus.SATISFIED, MatchStatus.SATISFIED, MatchStatus.UNKNOWN);
        // enum 은 문자열로, 판정 근거의 한글은 깨지지 않은 채로 왕복해야 한다.
        assertThat(reloaded.results().get(0).note()).isEqualTo("20Gbps / 요구 10Gbps 이상 충족");
        assertThat(reloaded.summary().satisfactionRate()).isEqualTo(66.7);
    }

    @Test
    @DisplayName("목록의 요구사항 건수·매칭 여부 집계 쿼리가 동작한다")
    void listAggregationWorks() {
        Long bidId = saveBid("20260900123", "A기관 IPS 도입", LocalDateTime.now().plusDays(10)).getId();
        requirementRepository.saveAll(List.of(
                requirement(bidId, "처리성능", "네트워크 처리량 10Gbps 이상", "10", "Gbps", true),
                requirement(bidId, "인증", "CC인증 EAL2 이상 필수", "EAL2", null, true)));
        matchService.match(bidId, saveProduct());

        BidSummaryResponse summary = bidService.search(null, "OPEN", PageRequest.of(0, 10)).content().get(0);

        assertThat(summary.requirementCount()).isEqualTo(2);
        assertThat(summary.matchStatus()).isEqualTo(BidSummaryResponse.MATCHED);
    }

    private BidAnnouncement saveBid(String bidNtceNo, String title, LocalDateTime closeDate) {
        return bidRepository.save(BidAnnouncement.builder()
                .bidNtceNo(bidNtceNo)
                .title(title)
                .institution("A기관")
                .closeDate(closeDate)
                .estimatedPrice(new BigDecimal("150000000"))
                .syncedAt(LocalDateTime.now())
                .build());
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

    private Long saveProduct() {
        ProductSpec product = ProductSpec.builder().productName("NGIPS-2000").category("IPS").build();
        product.addSpec(ProductSpecDetail.builder().specKey("처리성능").specValue("20").unit("Gbps").build());
        product.addSpec(ProductSpecDetail.builder().specKey("CC인증등급").specValue("EAL4").build());
        return productRepository.save(product).getId();
    }
}
