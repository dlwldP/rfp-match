package com.rfpmatch.product.controller;

import com.rfpmatch.product.dto.ProductRequest;
import com.rfpmatch.product.dto.ProductResponse;
import com.rfpmatch.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "제품 스펙", description = "자사 제품과 스펙 항목 관리")
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(summary = "제품 등록")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }

    @Operation(summary = "제품 목록 조회")
    @GetMapping
    public List<ProductResponse> list() {
        return productService.findAll();
    }

    @Operation(summary = "제품 상세 조회")
    @GetMapping("/{productId}")
    public ProductResponse detail(@PathVariable Long productId) {
        return productService.findOne(productId);
    }

    @Operation(summary = "제품 수정", description = "스펙을 통째로 교체한다. 수정 시 이 제품의 기존 매칭 결과는 무효화된다.")
    @PutMapping("/{productId}")
    public ProductResponse update(@PathVariable Long productId, @Valid @RequestBody ProductRequest request) {
        return productService.update(productId, request);
    }

    @Operation(summary = "제품 삭제")
    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long productId) {
        productService.delete(productId);
    }
}
