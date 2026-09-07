package com.rfpmatch.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rfpmatch.common.exception.ExternalApiException;
import com.rfpmatch.sync.client.BidNoticeApiClient;
import com.rfpmatch.sync.client.BidNoticeApiProperties;
import com.rfpmatch.sync.dto.BidNoticeItem;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@DisplayName("입찰공고 Open API 클라이언트")
class BidNoticeApiClientTest {

    private static final LocalDateTime FROM = LocalDateTime.of(2026, 9, 1, 0, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2026, 9, 30, 23, 59);

    private MockRestServiceServer server;
    private BidNoticeApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new BidNoticeApiClient(builder, new ObjectMapper(), properties());
    }

    @Test
    @DisplayName("응답을 공고 목록으로 변환한다")
    void fetch() {
        server.expect(requestTo(containsString("bidNtceNm=%EC%B9%A8%EC%9E%85%EB%B0%A9%EC%A7%80")))
                .andRespond(withSuccess(successBody(), MediaType.APPLICATION_JSON));

        List<BidNoticeItem> items = client.fetchByKeyword("침입방지", FROM, TO);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).bidNtceNo()).isEqualTo("20260900123");
        assertThat(items.get(0).bidNtceNm()).contains("침입방지시스템");
        server.verify();
    }

    @Test
    @DisplayName("items가 {\"item\": [...]} 로 한 번 더 감싸여 와도 읽는다")
    void fetchWrappedItems() {
        String body = """
                {"response":{"header":{"resultCode":"00","resultMsg":"NORMAL SERVICE"},
                 "body":{"items":{"item":[{"bidNtceNo":"20260900999","bidNtceNm":"방화벽 도입"}]},"totalCount":1}}}
                """;
        server.expect(requestTo(containsString("type=json")))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        List<BidNoticeItem> items = client.fetchByKeyword("방화벽", FROM, TO);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).bidNtceNo()).isEqualTo("20260900999");
    }

    @Test
    @DisplayName("서비스키 오류 등 JSON이 아닌 응답은 502로 올린다")
    void invalidServiceKey() {
        String xmlError = """
                <OpenAPI_ServiceResponse><cmmMsgHeader><returnAuthMsg>SERVICE_KEY_IS_NOT_REGISTERED_ERROR</returnAuthMsg>
                </cmmMsgHeader></OpenAPI_ServiceResponse>
                """;
        server.expect(requestTo(containsString("serviceKey")))
                .andRespond(withSuccess(xmlError, MediaType.APPLICATION_XML));

        assertThatThrownBy(() -> client.fetchByKeyword("침입방지", FROM, TO))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("해석할 수 없습니다");
    }

    @Test
    @DisplayName("정상이 아닌 resultCode는 502로 올린다")
    void errorResultCode() {
        String body = """
                {"response":{"header":{"resultCode":"30","resultMsg":"SERVICE KEY IS NOT REGISTERED ERROR"},"body":{}}}
                """;
        server.expect(requestTo(containsString("serviceKey")))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchByKeyword("침입방지", FROM, TO))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("[30]");
    }

    @Test
    @DisplayName("HTTP 오류도 502로 바꿔 올린다")
    void httpError() {
        server.expect(requestTo(containsString("serviceKey"))).andRespond(withServerError());

        assertThatThrownBy(() -> client.fetchByKeyword("침입방지", FROM, TO))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("호출 실패");
    }

    @Test
    @DisplayName("인증키가 없으면 호출하지 않고 바로 실패한다")
    void missingServiceKey() {
        BidNoticeApiProperties noKey = new BidNoticeApiProperties(true, "http://localhost/api", "getBidNoticeList",
                "", List.of("침입방지"), "1", 7, 100, 1, Duration.ofSeconds(5), "0 0 7 * * *");
        BidNoticeApiClient clientWithoutKey = new BidNoticeApiClient(RestClient.builder(), new ObjectMapper(), noKey);

        assertThatThrownBy(() -> clientWithoutKey.fetchByKeyword("침입방지", FROM, TO))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("serviceKey");
    }

    @Test
    @DisplayName("엔드포인트가 설정되지 않았으면 호출하지 않고 바로 실패한다")
    void missingEndpoint() {
        BidNoticeApiProperties noEndpoint = new BidNoticeApiProperties(true, "", "", "test-service-key",
                List.of("침입방지"), "1", 7, 100, 1, Duration.ofSeconds(5), "0 0 7 * * *");
        BidNoticeApiClient clientWithoutEndpoint =
                new BidNoticeApiClient(RestClient.builder(), new ObjectMapper(), noEndpoint);

        assertThatThrownBy(() -> clientWithoutEndpoint.fetchByKeyword("침입방지", FROM, TO))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("엔드포인트");
    }

    private BidNoticeApiProperties properties() {
        return new BidNoticeApiProperties(true, "http://example.test/openapi/BidNoticeService",
                "getBidNoticeList", "test-service-key", List.of("침입방지"), "1",
                7, 100, 1, Duration.ofSeconds(5), "0 0 7 * * *");
    }

    private String successBody() {
        return """
                {"response":{"header":{"resultCode":"00","resultMsg":"NORMAL SERVICE"},
                 "body":{"items":[{
                     "bidNtceNo":"20260900123",
                     "bidNtceOrd":"00",
                     "bidNtceNm":"A기관 네트워크 침입방지시스템(IPS) 도입",
                     "ntceInsttNm":"A기관",
                     "dminsttNm":"A기관",
                     "bidClseDt":"2026-09-25 17:00",
                     "presmptPrce":"150000000",
                     "unusedField":"무시되어야 한다"
                 }],"totalCount":1,"numOfRows":100,"pageNo":1}}}
                """;
    }
}
