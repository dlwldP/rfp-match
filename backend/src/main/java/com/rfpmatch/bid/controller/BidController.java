package com.rfpmatch.bid.controller;

import com.rfpmatch.bid.dto.BidDetailResponse;
import com.rfpmatch.bid.dto.BidSummaryResponse;
import com.rfpmatch.bid.service.BidService;
import com.rfpmatch.common.dto.PageResponse;
import com.rfpmatch.sync.service.BidSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "입찰공고", description = "외부 Open API로 수집한 공고 조회 및 수동 동기화")
@RestController
@RequestMapping("/api/v1/bids")
public class BidController {

    private static final int MAX_PAGE_SIZE = 100;

    private final BidService bidService;
    private final BidSyncService bidSyncService;

    public BidController(BidService bidService, BidSyncService bidSyncService) {
        this.bidService = bidService;
        this.bidSyncService = bidSyncService;
    }

    @Operation(summary = "입찰공고 목록 조회", description = "공고명 키워드와 진행상태(OPEN/CLOSED)로 필터링한다.")
    @GetMapping
    public PageResponse<BidSummaryResponse> list(@RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        return bidService.search(keyword, status, pageable);
    }

    @Operation(summary = "입찰공고 상세 조회", description = "공고 정보와 등록된 요구사항 항목을 함께 반환한다.")
    @GetMapping("/{bidId}")
    public BidDetailResponse detail(@PathVariable Long bidId) {
        return bidService.findDetail(bidId);
    }

    @Operation(summary = "입찰공고 수동 동기화",
            description = "스케줄러를 기다리지 않고 즉시 수집한다. 외부 API 실패 시 502를 반환하고 기존 데이터는 유지된다.")
    @PostMapping("/sync")
    public BidSyncService.SyncReport sync() {
        return bidSyncService.sync();
    }
}
