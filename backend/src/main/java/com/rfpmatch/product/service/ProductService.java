package com.rfpmatch.product.service;

import com.rfpmatch.common.exception.NotFoundException;
import com.rfpmatch.match.repository.MatchResultRepository;
import com.rfpmatch.product.domain.ProductSpec;
import com.rfpmatch.product.dto.ProductRequest;
import com.rfpmatch.product.dto.ProductResponse;
import com.rfpmatch.product.repository.ProductSpecRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductSpecRepository productRepository;
    private final MatchResultRepository matchResultRepository;

    public ProductService(ProductSpecRepository productRepository,
                          MatchResultRepository matchResultRepository) {
        this.productRepository = productRepository;
        this.matchResultRepository = matchResultRepository;
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        return ProductResponse.from(productRepository.save(request.toEntity()));
    }

    public List<ProductResponse> findAll() {
        return productRepository.findAllWithSpecs().stream().map(ProductResponse::from).toList();
    }

    public ProductResponse findOne(Long productId) {
        return ProductResponse.from(getProduct(productId));
    }

    @Transactional
    public ProductResponse update(Long productId, ProductRequest request) {
        ProductSpec product = getProduct(productId);
        product.updateBasicInfo(request.productName().trim(), request.category().trim());
        product.replaceSpecs(request.toSpecEntities());

        // 스펙이 바뀌면 이 제품으로 돌린 갭분석 결과는 무효다. 지우고 다시 매칭하게 한다.
        matchResultRepository.deleteByProductId(productId);
        return ProductResponse.from(product);
    }

    @Transactional
    public void delete(Long productId) {
        ProductSpec product = getProduct(productId);
        matchResultRepository.deleteByProductId(productId);
        productRepository.delete(product);
    }

    public ProductSpec getProduct(Long productId) {
        return productRepository.findByIdWithSpecs(productId)
                .orElseThrow(() -> NotFoundException.product(productId));
    }
}
