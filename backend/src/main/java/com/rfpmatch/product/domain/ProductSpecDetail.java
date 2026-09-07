package com.rfpmatch.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 제품 스펙 한 줄. 예: 처리성능 = 20 Gbps */
@Entity
@Table(name = "product_spec_detail")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductSpecDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductSpec product;

    @Column(name = "spec_key", nullable = false, length = 100)
    private String specKey;

    @Column(name = "spec_value", nullable = false, length = 500)
    private String specValue;

    @Column(length = 50)
    private String unit;

    @Builder
    private ProductSpecDetail(String specKey, String specValue, String unit) {
        this.specKey = specKey;
        this.specValue = specValue;
        this.unit = unit;
    }

    void assignTo(ProductSpec product) {
        this.product = product;
    }

    /** 사람이 읽는 형태의 값. 예: "20Gbps" */
    public String displayValue() {
        return unit == null || unit.isBlank() ? specValue : specValue + unit;
    }
}
