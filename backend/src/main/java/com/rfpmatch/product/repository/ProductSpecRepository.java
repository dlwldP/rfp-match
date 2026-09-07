package com.rfpmatch.product.repository;

import com.rfpmatch.product.domain.ProductSpec;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductSpecRepository extends JpaRepository<ProductSpec, Long> {

    /** 목록 화면에서 제품마다 스펙 쿼리가 따로 나가지 않도록 한 번에 가져온다. */
    @Query("select distinct p from ProductSpec p left join fetch p.specs order by p.id")
    List<ProductSpec> findAllWithSpecs();

    @Query("select p from ProductSpec p left join fetch p.specs where p.id = :id")
    Optional<ProductSpec> findByIdWithSpecs(Long id);
}
