package com.rfpmatch.product.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 자사 제품 한 대. 스펙 항목(ProductSpecDetail)을 자식으로 갖는다. */
@Entity
@Table(name = "product_spec")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductSpec {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    /** 제품군. 예: IPS / DDoS / UTM */
    @Column(nullable = false, length = 100)
    private String category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<ProductSpecDetail> specs = new ArrayList<>();

    @Builder
    private ProductSpec(String productName, String category) {
        this.productName = productName;
        this.category = category;
    }

    public List<ProductSpecDetail> getSpecs() {
        return Collections.unmodifiableList(specs);
    }

    public void addSpec(ProductSpecDetail detail) {
        detail.assignTo(this);
        this.specs.add(detail);
    }

    public void replaceSpecs(List<ProductSpecDetail> details) {
        this.specs.clear();
        details.forEach(this::addSpec);
    }

    public void updateBasicInfo(String productName, String category) {
        this.productName = productName;
        this.category = category;
    }
}
