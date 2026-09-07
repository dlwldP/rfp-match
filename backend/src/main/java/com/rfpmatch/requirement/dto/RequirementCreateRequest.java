package com.rfpmatch.requirement.dto;

import com.rfpmatch.requirement.domain.RequirementItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/** RFP 원문에서 뽑은 요구사항 항목들을 한 번에 등록한다. */
public record RequirementCreateRequest(
        @NotEmpty(message = "등록할 요구사항 항목이 최소 1건 필요합니다.")
        @Valid List<Item> items) {

    public record Item(
            @NotBlank(message = "category는 비어있을 수 없습니다.")
            @Size(max = 100) String category,

            @NotBlank(message = "description은 비어있을 수 없습니다.")
            @Size(max = 1000) String description,

            @NotBlank(message = "requiredValue는 비어있을 수 없습니다.")
            @Size(max = 200) String requiredValue,

            @Size(max = 50) String unit,

            Boolean mandatory) {

        public RequirementItem toEntity(Long bidId) {
            return RequirementItem.builder()
                    .bidId(bidId)
                    .category(category.trim())
                    .description(description.trim())
                    .requiredValue(requiredValue.trim())
                    .unit(unit == null || unit.isBlank() ? null : unit.trim())
                    // 명시하지 않으면 필수 요구사항으로 본다 (놓치면 실격이므로 보수적으로).
                    .mandatory(mandatory == null || mandatory)
                    .build();
        }
    }
}
