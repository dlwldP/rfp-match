package com.rfpmatch.sync.service;

import com.rfpmatch.sync.client.BidNoticeApiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 입찰공고를 주기적으로 수집한다(기본 1일 2회).
 * 실패는 로그만 남기고 넘어간다 — 다음 주기에 다시 시도하며 그 사이에도 화면은 마지막 캐시로 동작한다.
 */
@Slf4j
@Component
public class BidSyncScheduler {

    private final BidSyncService bidSyncService;
    private final BidNoticeApiProperties properties;

    public BidSyncScheduler(BidSyncService bidSyncService, BidNoticeApiProperties properties) {
        this.bidSyncService = bidSyncService;
        this.properties = properties;
    }

    @Scheduled(cron = "${bid-api.cron:0 0 7,19 * * *}", zone = "Asia/Seoul")
    public void syncBidAnnouncements() {
        if (!properties.enabled()) {
            log.debug("공고 동기화가 비활성화되어 있습니다. (bid-api.enabled=false)");
            return;
        }
        if (!properties.hasServiceKey()) {
            log.warn("serviceKey 가 없어 동기화를 건너뜁니다.");
            return;
        }
        try {
            bidSyncService.sync();
        } catch (Exception e) {
            log.error("입찰공고 동기화 실패 — 마지막 수집 데이터를 유지합니다.", e);
        }
    }
}
