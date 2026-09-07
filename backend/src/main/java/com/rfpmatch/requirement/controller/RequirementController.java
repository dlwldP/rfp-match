package com.rfpmatch.requirement.controller;

import com.rfpmatch.requirement.dto.RequirementCreateRequest;
import com.rfpmatch.requirement.dto.RequirementCreateResponse;
import com.rfpmatch.requirement.dto.RequirementItemResponse;
import com.rfpmatch.requirement.service.RequirementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "요구사항", description = "RFP 원문에서 추출한 공고별 요구사항 항목 관리")
@RestController
@RequestMapping("/api/v1/bids/{bidId}/requirements")
public class RequirementController {

    private final RequirementService requirementService;

    public RequirementController(RequirementService requirementService) {
        this.requirementService = requirementService;
    }

    @Operation(summary = "요구사항 항목 등록",
            description = "RFP 첨부파일은 API로 구조화되어 오지 않으므로 담당자가 읽고 항목을 등록한다. 등록 시 기존 매칭 결과는 무효화된다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RequirementCreateResponse register(@PathVariable Long bidId,
                                              @Valid @RequestBody RequirementCreateRequest request) {
        return requirementService.register(bidId, request);
    }

    @Operation(summary = "요구사항 항목 목록 조회")
    @GetMapping
    public List<RequirementItemResponse> list(@PathVariable Long bidId) {
        return requirementService.findByBid(bidId);
    }

    @Operation(summary = "요구사항 항목 삭제")
    @DeleteMapping("/{requirementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long bidId, @PathVariable Long requirementId) {
        requirementService.delete(bidId, requirementId);
    }
}
