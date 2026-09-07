package com.rfpmatch.match.controller;

import com.rfpmatch.match.dto.MatchResponse;
import com.rfpmatch.match.service.MatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "매칭/갭분석", description = "요구사항과 제품 스펙 대조 실행 및 결과 조회")
@RestController
@RequestMapping("/api/v1/bids/{bidId}")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @Operation(summary = "매칭 실행",
            description = "공고의 요구사항 전체를 지정한 제품 스펙과 대조해 갭분석표를 만든다. 재실행하면 이전 결과를 덮어쓴다.")
    @PostMapping("/match")
    public MatchResponse match(@PathVariable Long bidId, @RequestParam Long productId) {
        return matchService.match(bidId, productId);
    }

    @Operation(summary = "갭분석 결과 조회", description = "마지막으로 실행한 매칭 결과를 다시 조회한다.")
    @GetMapping("/match-result")
    public MatchResponse result(@PathVariable Long bidId, @RequestParam Long productId) {
        return matchService.findResult(bidId, productId);
    }
}
