package com.rfpmatch.product.dto;

import com.rfpmatch.product.domain.ProductSpec;
import com.rfpmatch.product.domain.ProductSpecDetail;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 자사 제품 등록/수정 요청. */
public record ProductRequest(
        @NotBlank(message = "productName은 비어있을 수 없습니다.")
        @Size(max = 200) String productName,

        @NotBlank(message = "category는 비어있을 수 없습니다.")
        @Size(max = 100) String category,

        @NotEmpty(message = "제품 스펙이 최소 1건 필요합니다.")
        @Valid List<SpecRequest> specs) {

    public record SpecRequest(
            @NotBlank(message = "specKey는 비어있을 수 없습니다.")
            @Size(max = 100) String specKey,

            @NotBlank(message = "specValue는 비어있을 수 없습니다.")
            @Size(max = 500) String specValue,

            @Size(max = 50) String unit) {

        public ProductSpecDetail toEntity() {
            return ProductSpecDetail.builder()
                    .specKey(specKey.trim())
                    .specValue(specValue.trim())
                    .unit(unit == null || unit.isBlank() ? null : unit.trim())
                    .build();
        }
    }

    public ProductSpec toEntity() {
        ProductSpec product = ProductSpec.builder()
                .productName(productName.trim())
                .category(category.trim())
                .build();
        specs.stream().map(SpecRequest::toEntity).forEach(product::addSpec);
        return product;
    }

    public List<ProductSpecDetail> toSpecEntities() {
        return specs.stream().map(SpecRequest::toEntity).toList();
    }
}
