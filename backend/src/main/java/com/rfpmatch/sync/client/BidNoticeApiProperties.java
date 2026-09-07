package com.rfpmatch.sync.client;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 공공 입찰공고 Open API 연동 설정.
 *
 * <p>엔드포인트와 인증키는 코드에 두지 않고 전부 환경변수/외부 설정으로 주입한다
 * ({@code BID_API_BASE_URL}, {@code BID_API_OPERATION}, {@code BID_API_SERVICE_KEY}).
 * 인증키는 URL 인코딩 전의 원본 값을 넣는다 — 인코딩은 클라이언트가 한 번만 수행하므로
 * 이미 인코딩된 키를 넣으면 이중 인코딩으로 실패한다.
 *
 * @param enabled      스케줄러 동작 여부. 인증키가 없는 로컬 개발에서는 false 로 둔다.
 * @param baseUrl      서비스 base URL (설정으로 주입)
 * @param operation    오퍼레이션 경로. 업무구분(물품/용역/공사)별로 다르다.
 * @param serviceKey   발급받은 인증키(인코딩 전 원본)
 * @param keywords     공고명 검색 키워드. 키워드별로 한 번씩 조회한다.
 * @param inqryDiv     조회구분 (1: 공고게시일시 기준)
 * @param lookbackDays 조회 시작일을 오늘로부터 며칠 전으로 잡을지
 * @param pageSize     한 번에 가져올 건수
 * @param maxPages     키워드당 최대 페이지 수. 과도한 호출을 막는 안전장치
 * @param timeout      연결/응답 타임아웃
 * @param cron         동기화 스케줄 (기본: 매일 07시, 19시)
 */
@ConfigurationProperties(prefix = "bid-api")
public record BidNoticeApiProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("") String baseUrl,
        @DefaultValue("") String operation,
        @DefaultValue("") String serviceKey,
        @DefaultValue({"침입방지", "침입차단", "방화벽", "DDoS", "보안장비", "통합보안"}) List<String> keywords,
        @DefaultValue("1") String inqryDiv,
        @DefaultValue("7") int lookbackDays,
        @DefaultValue("100") int pageSize,
        @DefaultValue("5") int maxPages,
        @DefaultValue("10s") Duration timeout,
        @DefaultValue("0 0 7,19 * * *") String cron) {

    public boolean hasServiceKey() {
        return serviceKey != null && !serviceKey.isBlank();
    }

    public boolean hasEndpoint() {
        return baseUrl != null && !baseUrl.isBlank() && operation != null && !operation.isBlank();
    }

    public String endpoint() {
        return baseUrl + "/" + operation;
    }
}
