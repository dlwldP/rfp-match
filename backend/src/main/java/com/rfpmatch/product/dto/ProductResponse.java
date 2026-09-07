package com.rfpmatch.product.dto;

import com.rfpmatch.product.domain.ProductSpec;
import com.rfpmatch.product.domain.ProductSpecDetail;
import java.util.List;

public record ProductResponse(
        Long productId,
        String productName,
        String category,
        List<SpecResponse> specs) {

    public record SpecResponse(Long specId, String specKey, String specValue, String unit) {

        public static SpecResponse from(ProductSpecDetail detail) {
            return new SpecResponse(detail.getId(), detail.getSpecKey(), detail.getSpecValue(), detail.getUnit());
        }
    }

    public static ProductResponse from(ProductSpec product) {
        return new ProductResponse(
                product.getId(),
                product.getProductName(),
                product.getCategory(),
                product.getSpecs().stream().map(SpecResponse::from).toList());
    }
}
