package com.rfpmatch.sync.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rfpmatch.common.exception.ExternalApiException;
import com.rfpmatch.sync.dto.BidNoticeItem;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 공공 입찰공고 Open API 호출 담당.
 *
 * <p>공공 오픈 API 응답은 오퍼레이션마다 items 가 배열로 오기도 하고
 * {@code {"item": [...]}} 로 한 번 더 감싸여 오기도 한다. 인증키가 틀리면 JSON 대신
 * XML 에러 문서가 내려오기도 해서, 응답을 트리로 먼저 훑은 뒤 필요한 부분만 DTO 로 변환한다.
 */
@Slf4j
@Component
public class BidNoticeApiClient {

    private static final DateTimeFormatter INQUIRY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final String NORMAL_RESULT_CODE = "00";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final BidNoticeApiProperties properties;

    public BidNoticeApiClient(RestClient.Builder restClientBuilder,
                              ObjectMapper objectMapper,
                              BidNoticeApiProperties properties) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /** 공고명에 키워드가 들어간 공고를 조회 기간만큼 페이지를 넘겨 가며 모두 가져온다. */
    public List<BidNoticeItem> fetchByKeyword(String keyword, LocalDateTime from, LocalDateTime to) {
        if (!properties.hasEndpoint()) {
            throw new ExternalApiException("입찰공고 API 엔드포인트가 설정되지 않았습니다. (bid-api.base-url / bid-api.operation)");
        }
        if (!properties.hasServiceKey()) {
            throw new ExternalApiException("입찰공고 API serviceKey 가 설정되지 않았습니다. (bid-api.service-key)");
        }

        List<BidNoticeItem> collected = new ArrayList<>();
        for (int pageNo = 1; pageNo <= properties.maxPages(); pageNo++) {
            Page page = requestPage(keyword, from, to, pageNo);
            collected.addAll(page.items());
            if (collected.size() >= page.totalCount() || page.items().isEmpty()) {
                break;
            }
        }
        log.debug("입찰공고 조회 완료: keyword={}, 수집건수={}", keyword, collected.size());
        return collected;
    }

    private record Page(List<BidNoticeItem> items, int totalCount) {
    }

    private Page requestPage(String keyword, LocalDateTime from, LocalDateTime to, int pageNo) {
        URI uri = buildUri(keyword, from, to, pageNo);
        String body;
        try {
            body = restClient.get().uri(uri).retrieve().body(String.class);
        } catch (RestClientException e) {
            throw new ExternalApiException("입찰공고 API 호출 실패: " + e.getMessage(), e);
        }
        return parse(body, keyword);
    }

    /** 쿼리 파라미터 이름은 연동하는 API 명세를 따른다. */
    private URI buildUri(String keyword, LocalDateTime from, LocalDateTime to, int pageNo) {
        Map<String, String> params = new LinkedHashMap<>();
        // 인증키는 원본 값을 여기서 한 번만 인코딩한다 (인코딩된 키를 넣으면 이중 인코딩으로 실패).
        params.put("serviceKey", properties.serviceKey());
        params.put("pageNo", String.valueOf(pageNo));
        params.put("numOfRows", String.valueOf(properties.pageSize()));
        params.put("inqryDiv", properties.inqryDiv());
        params.put("inqryBgnDt", from.format(INQUIRY_FORMAT));
        params.put("inqryEndDt", to.format(INQUIRY_FORMAT));
        params.put("bidNtceNm", keyword);
        params.put("type", "json");

        StringBuilder query = new StringBuilder();
        params.forEach((key, value) -> {
            if (!query.isEmpty()) {
                query.append('&');
            }
            query.append(key).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        });
        return URI.create(properties.endpoint() + "?" + query);
    }

    /** 응답 본문을 공고 목록으로 바꾼다. 형태가 예상과 다르면 502로 올린다. */
    private Page parse(String body, String keyword) {
        if (body == null || body.isBlank()) {
            throw new ExternalApiException("입찰공고 API 응답이 비어 있습니다. keyword=" + keyword);
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (Exception e) {
            // 인증키 오류 등은 XML 에러 문서로 내려온다.
            throw new ExternalApiException("입찰공고 API 응답을 해석할 수 없습니다: " + summarize(body), e);
        }

        JsonNode header = root.path("response").path("header");
        String resultCode = header.path("resultCode").asText("");
        if (!resultCode.isEmpty() && !NORMAL_RESULT_CODE.equals(resultCode)) {
            throw new ExternalApiException("입찰공고 API 오류 응답: [%s] %s"
                    .formatted(resultCode, header.path("resultMsg").asText("")));
        }

        JsonNode bodyNode = root.path("response").path("body");
        JsonNode itemsNode = bodyNode.path("items");
        if (itemsNode.isObject()) {
            itemsNode = itemsNode.path("item"); // items:{item:[...]} 형태 대응
        }

        List<BidNoticeItem> items = new ArrayList<>();
        if (itemsNode.isArray()) {
            itemsNode.forEach(node -> items.add(objectMapper.convertValue(node, BidNoticeItem.class)));
        } else if (itemsNode.isObject()) {
            items.add(objectMapper.convertValue(itemsNode, BidNoticeItem.class)); // 1건만 오는 경우
        }

        return new Page(items, bodyNode.path("totalCount").asInt(items.size()));
    }

    /** 오류 응답을 로그/메시지에 남길 때 인증키 등이 그대로 노출되지 않도록 앞부분만 요약한다. */
    private String summarize(String body) {
        String flattened = body.replaceAll("\\s+", " ").trim();
        return flattened.length() > 200 ? flattened.substring(0, 200) + "..." : flattened;
    }
}
