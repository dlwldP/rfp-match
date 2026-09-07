package com.rfpmatch.sync.service;

import com.rfpmatch.bid.domain.BidAnnouncement;
import com.rfpmatch.bid.repository.BidAnnouncementRepository;
import com.rfpmatch.common.exception.ExternalApiException;
import com.rfpmatch.sync.client.BidNoticeApiClient;
import com.rfpmatch.sync.client.BidNoticeApiProperties;
import com.rfpmatch.sync.dto.BidNoticeItem;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 외부 Open API에서 받아온 공고를 DB에 반영한다.
 *
 * <p>실시간 조회 대신 주기 수집 후 캐싱하는 구조라, 외부 API가 죽어도 화면은 마지막 수집분으로 계속 돈다.
 * 키워드 하나가 실패해도 나머지 키워드 수집은 계속 진행하고, 실패한 키워드만 리포트에 남긴다.
 */
@Slf4j
@Service
public class BidSyncService {

    private static final List<DateTimeFormatter> CLOSE_DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyyMMddHHmm"));

    private final BidNoticeApiClient client;
    private final BidNoticeApiProperties properties;
    private final BidAnnouncementRepository bidRepository;

    public BidSyncService(BidNoticeApiClient client,
                          BidNoticeApiProperties properties,
                          BidAnnouncementRepository bidRepository) {
        this.client = client;
        this.properties = properties;
        this.bidRepository = bidRepository;
    }

    /**
     * 설정된 키워드로 최근 공고를 수집해 저장한다.
     *
     * @throws ExternalApiException 모든 키워드 조회가 실패한 경우 (기존 캐시는 그대로 둔다)
     */
    @Transactional
    public SyncReport sync() {
        LocalDateTime to = LocalDateTime.now();
        LocalDateTime from = to.minusDays(properties.lookbackDays());

        Map<String, BidNoticeItem> collected = new LinkedHashMap<>();
        List<String> failedKeywords = new ArrayList<>();

        for (String keyword : properties.keywords()) {
            try {
                client.fetchByKeyword(keyword, from, to).stream()
                        .filter(item -> item.bidNtceNo() != null && !item.bidNtceNo().isBlank())
                        // 키워드가 여러 개라 같은 공고가 중복으로 잡힌다. 공고번호로 한 번 걸러낸다.
                        .forEach(item -> collected.putIfAbsent(item.bidNtceNo(), item));
            } catch (ExternalApiException e) {
                log.warn("키워드 '{}' 수집 실패 — 나머지 키워드는 계속 진행합니다. 원인: {}", keyword, e.getMessage());
                failedKeywords.add(keyword);
            }
        }

        if (!failedKeywords.isEmpty() && failedKeywords.size() == properties.keywords().size()) {
            throw new ExternalApiException(
                    "입찰공고 수집에 모두 실패했습니다. 마지막 수집 데이터를 유지합니다. keywords=" + failedKeywords);
        }

        int created = 0;
        int updated = 0;
        for (BidNoticeItem item : collected.values()) {
            BidAnnouncement fresh = toEntity(item, to);
            Optional<BidAnnouncement> existing = bidRepository.findByBidNtceNo(fresh.getBidNtceNo());
            if (existing.isPresent()) {
                existing.get().syncFrom(fresh);
                updated++;
            } else {
                bidRepository.save(fresh);
                created++;
            }
        }

        SyncReport report = new SyncReport(collected.size(), created, updated, failedKeywords);
        log.info("입찰공고 동기화 완료: {}", report);
        return report;
    }

    private BidAnnouncement toEntity(BidNoticeItem item, LocalDateTime syncedAt) {
        return BidAnnouncement.builder()
                .bidNtceNo(item.bidNtceNo())
                .title(blankToDefault(item.bidNtceNm(), "(공고명 없음)"))
                .institution(item.ntceInsttNm())
                .demandInstitution(item.dminsttNm())
                .closeDate(parseDateTime(item.bidClseDt()))
                .estimatedPrice(parsePrice(item.presmptPrce()))
                .noticeUrl(item.bidNtceDtlUrl())
                .syncedAt(syncedAt)
                .build();
    }

    /** 오퍼레이션마다 일시 표기가 달라 알려진 포맷을 차례로 시도한다. */
    static LocalDateTime parseDateTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        for (DateTimeFormatter format : CLOSE_DATE_FORMATS) {
            try {
                return LocalDateTime.parse(value, format);
            } catch (DateTimeParseException ignored) {
                // 다음 포맷 시도
            }
        }
        log.debug("입찰마감일시를 해석하지 못했습니다: {}", raw);
        return null;
    }

    /** "150,000,000" / "150000000.00" / "" 을 모두 받아 준다. */
    static BigDecimal parsePrice(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String digits = raw.replaceAll("[^0-9.]", "");
        if (digits.isBlank() || digits.equals(".")) {
            return null;
        }
        try {
            return new BigDecimal(digits).setScale(0, java.math.RoundingMode.DOWN);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    /**
     * 동기화 1회 결과.
     *
     * @param fetched        중복 제거 후 수집 건수
     * @param created        신규 저장 건수
     * @param updated        기존 공고 갱신 건수
     * @param failedKeywords 조회에 실패한 키워드
     */
    public record SyncReport(int fetched, int created, int updated, List<String> failedKeywords) {
    }
}
