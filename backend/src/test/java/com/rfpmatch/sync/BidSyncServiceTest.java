package com.rfpmatch.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.rfpmatch.bid.domain.BidAnnouncement;
import com.rfpmatch.bid.repository.BidAnnouncementRepository;
import com.rfpmatch.common.exception.ExternalApiException;
import com.rfpmatch.sync.client.BidNoticeApiClient;
import com.rfpmatch.sync.dto.BidNoticeItem;
import com.rfpmatch.sync.service.BidSyncService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@TestPropertySource(properties = {
        "bid-api.service-key=test-key",
        "bid-api.keywords[0]=침입방지",
        "bid-api.keywords[1]=방화벽"
})
@DisplayName("입찰공고 동기화")
class BidSyncServiceTest {

    @MockitoBean
    private BidNoticeApiClient client;

    @Autowired
    private BidSyncService bidSyncService;

    @Autowired
    private BidAnnouncementRepository bidRepository;

    @BeforeEach
    void setUp() {
        bidRepository.deleteAll();
    }

    @Test
    @DisplayName("수집한 공고를 저장하고, 같은 공고를 다시 받으면 갱신한다")
    void syncAndResync() {
        given(client.fetchByKeyword(eq("침입방지"), any(), any())).willReturn(List.of(
                item("20260900123", "A기관 IPS 도입", "2026-09-25 17:00", "150000000")));
        given(client.fetchByKeyword(eq("방화벽"), any(), any())).willReturn(List.of(
                item("20260900456", "B기관 방화벽 교체", "202609201100", "92,000,000")));

        BidSyncService.SyncReport first = bidSyncService.sync();

        assertThat(first.fetched()).isEqualTo(2);
        assertThat(first.created()).isEqualTo(2);
        assertThat(first.updated()).isZero();

        BidAnnouncement saved = bidRepository.findByBidNtceNo("20260900123").orElseThrow();
        assertThat(saved.getCloseDate()).isEqualTo(LocalDateTime.of(2026, 9, 25, 17, 0));
        assertThat(saved.getEstimatedPrice()).isEqualByComparingTo(new BigDecimal("150000000"));
        assertThat(bidRepository.findByBidNtceNo("20260900456").orElseThrow().getCloseDate())
                .isEqualTo(LocalDateTime.of(2026, 9, 20, 11, 0));

        // 공고명이 바뀐 채로 다시 내려오면 새 행이 아니라 기존 행이 갱신되어야 한다.
        given(client.fetchByKeyword(eq("침입방지"), any(), any())).willReturn(List.of(
                item("20260900123", "A기관 IPS 도입(정정공고)", "2026-09-28 17:00", "150000000")));

        BidSyncService.SyncReport second = bidSyncService.sync();

        assertThat(second.updated()).isEqualTo(2);
        assertThat(second.created()).isZero();
        assertThat(bidRepository.count()).isEqualTo(2);
        assertThat(bidRepository.findByBidNtceNo("20260900123").orElseThrow().getTitle()).contains("정정공고");
    }

    @Test
    @DisplayName("키워드 하나가 실패해도 나머지는 계속 수집한다")
    void partialFailure() {
        given(client.fetchByKeyword(eq("침입방지"), any(), any()))
                .willThrow(new ExternalApiException("타임아웃"));
        given(client.fetchByKeyword(eq("방화벽"), any(), any())).willReturn(List.of(
                item("20260900456", "B기관 방화벽 교체", "2026-09-20 11:00", "92000000")));

        BidSyncService.SyncReport report = bidSyncService.sync();

        assertThat(report.failedKeywords()).containsExactly("침입방지");
        assertThat(report.created()).isEqualTo(1);
        assertThat(bidRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("모든 키워드가 실패하면 502로 올리고 기존 데이터는 그대로 둔다")
    void totalFailureKeepsCache() {
        bidRepository.save(BidAnnouncement.builder()
                .bidNtceNo("20260800001")
                .title("이전에 수집한 공고")
                .syncedAt(LocalDateTime.now().minusDays(1))
                .build());
        given(client.fetchByKeyword(any(), any(), any())).willThrow(new ExternalApiException("연결 실패"));

        assertThatThrownBy(() -> bidSyncService.sync())
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("마지막 수집 데이터를 유지");
        assertThat(bidRepository.findByBidNtceNo("20260800001")).isPresent();
    }

    @Test
    @DisplayName("여러 키워드에 같은 공고가 걸리면 한 번만 저장한다")
    void deduplicate() {
        BidNoticeItem duplicated = item("20260900777", "통합보안장비 구매", "2026-09-30 17:00", "50000000");
        given(client.fetchByKeyword(any(), any(), any())).willReturn(List.of(duplicated));

        BidSyncService.SyncReport report = bidSyncService.sync();

        assertThat(report.fetched()).isEqualTo(1);
        assertThat(bidRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("해석할 수 없는 마감일시나 가격은 비워 두고 저장은 계속한다")
    void tolerateBrokenFields() {
        given(client.fetchByKeyword(any(), any(), any())).willReturn(List.of(
                item("20260900888", "보안장비 구매", "일정 추후 공지", "별도 협의")));

        bidSyncService.sync();

        Optional<BidAnnouncement> saved = bidRepository.findByBidNtceNo("20260900888");
        assertThat(saved).isPresent();
        assertThat(saved.get().getCloseDate()).isNull();
        assertThat(saved.get().getEstimatedPrice()).isNull();
    }

    private BidNoticeItem item(String no, String name, String closeDate, String price) {
        return new BidNoticeItem(no, "00", name, "발주기관", "수요기관", closeDate, price, null);
    }
}
