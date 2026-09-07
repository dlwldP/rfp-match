package com.rfpmatch.match.service;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rfpmatch.bid.domain.BidAnnouncement;
import com.rfpmatch.bid.repository.BidAnnouncementRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** 공고 등록 → 요구사항 입력 → 제품 등록 → 매칭 실행 → 갭분석 조회까지 실제 API 흐름을 확인한다. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("갭분석 API 통합")
class MatchApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BidAnnouncementRepository bidRepository;

    private Long bidId;

    @BeforeEach
    void setUp() {
        bidRepository.deleteAll();
        bidId = bidRepository.save(BidAnnouncement.builder()
                .bidNtceNo("20260900123")
                .title("A기관 네트워크 침입방지시스템(IPS) 도입")
                .institution("A기관")
                .closeDate(LocalDateTime.now().plusDays(10))
                .estimatedPrice(new BigDecimal("150000000"))
                .syncedAt(LocalDateTime.now())
                .build()).getId();
    }

    @Test
    @DisplayName("요구사항을 등록하고 제품과 매칭하면 갭분석표가 나온다")
    void fullFlow() throws Exception {
        registerRequirements();
        Long productId = registerProduct();

        mockMvc.perform(post("/api/v1/bids/{bidId}/match", bidId).param("productId", productId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("NGIPS-2000"))
                .andExpect(jsonPath("$.results.length()").value(4))
                .andExpect(jsonPath("$.results[0].status").value("충족"))       // 20Gbps >= 10Gbps
                .andExpect(jsonPath("$.results[1].status").value("충족"))       // EAL4 >= EAL2
                .andExpect(jsonPath("$.results[2].status").value("미충족"))     // 국정원 검증필 미보유
                .andExpect(jsonPath("$.results[3].status").value("확인불가"))   // 대응 스펙 없음
                .andExpect(jsonPath("$.summary.totalCount").value(4))
                .andExpect(jsonPath("$.summary.satisfied").value(2))
                .andExpect(jsonPath("$.summary.unsatisfied").value(1))
                .andExpect(jsonPath("$.summary.unknown").value(1))
                .andExpect(jsonPath("$.summary.mandatoryUnsatisfied").value(1))
                .andExpect(jsonPath("$.summary.biddable").value(false));

        // 저장된 결과를 그대로 다시 조회할 수 있어야 한다.
        mockMvc.perform(get("/api/v1/bids/{bidId}/match-result", bidId).param("productId", productId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.satisfied").value(2));
    }

    @Test
    @DisplayName("목록에는 요구사항 건수와 매칭 여부가 함께 나온다")
    void bidListShowsProgress() throws Exception {
        mockMvc.perform(get("/api/v1/bids").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].requirementCount").value(0))
                .andExpect(jsonPath("$.content[0].matchStatus").value("미매칭"));

        registerRequirements();
        Long productId = registerProduct();
        mockMvc.perform(post("/api/v1/bids/{bidId}/match", bidId).param("productId", productId.toString()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/bids").param("keyword", "침입방지"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].requirementCount").value(4))
                .andExpect(jsonPath("$.content[0].matchStatus").value("매칭완료"))
                .andExpect(jsonPath("$.content[0].status").value("OPEN"));
    }

    @Test
    @DisplayName("요구사항 없이 매칭하면 400과 안내 메시지를 준다")
    void matchWithoutRequirements() throws Exception {
        Long productId = registerProduct();

        mockMvc.perform(post("/api/v1/bids/{bidId}/match", bidId).param("productId", productId.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("요구사항")))
                .andExpect(jsonPath("$.path").value("/api/v1/bids/%d/match".formatted(bidId)));
    }

    @Test
    @DisplayName("없는 공고를 조회하면 404 에러 포맷으로 응답한다")
    void bidNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/bids/{bidId}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("필수값이 빠진 요구사항 등록은 400으로 막는다")
    void validationError() throws Exception {
        String body = """
                {"items":[{"category":"처리성능","description":"네트워크 처리량","requiredValue":"","unit":"Gbps"}]}
                """;

        mockMvc.perform(post("/api/v1/bids/{bidId}/requirements", bidId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("requiredValue")));
    }

    private void registerRequirements() throws Exception {
        String body = """
                {"items":[
                  {"category":"처리성능","description":"네트워크 처리량 10Gbps 이상","requiredValue":"10","unit":"Gbps","mandatory":true},
                  {"category":"인증","description":"CC인증 EAL2 이상 필수","requiredValue":"EAL2","mandatory":true},
                  {"category":"인증","description":"국가정보원 보안적합성 검증필 제품","requiredValue":"YES","mandatory":true},
                  {"category":"유지보수","description":"무상 유지보수 3년 이상","requiredValue":"3","unit":"년","mandatory":false}
                ]}
                """;

        mockMvc.perform(post("/api/v1/bids/{bidId}/requirements", bidId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requirementIds.length()").value(4));
    }

    private Long registerProduct() throws Exception {
        String body = """
                {"productName":"NGIPS-2000","category":"IPS","specs":[
                  {"specKey":"처리성능","specValue":"20","unit":"Gbps"},
                  {"specKey":"CC인증등급","specValue":"EAL4"},
                  {"specKey":"국정원검증필","specValue":"NO"}
                ]}
                """;

        String response = mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("productId").asLong();
    }
}
