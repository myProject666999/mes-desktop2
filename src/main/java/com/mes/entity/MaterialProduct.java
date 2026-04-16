package com.mes.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "material_product")
@EqualsAndHashCode(exclude = {"category", "bomItems", "usedInBoms"})
@ToString(exclude = {"category", "bomItems", "usedInBoms"})
public class MaterialProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "category_id")
    private Long categoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private MaterialCategory category;

    private String specification;

    private String model;

    private String unit;

    private String description;

    @Column(name = "material_type")
    @Enumerated(EnumType.STRING)
    private MaterialType materialType = MaterialType.MATERIAL;

    @Column(name = "min_stock")
    private BigDecimal minStock = BigDecimal.ZERO;

    @Column(name = "max_stock")
    private BigDecimal maxStock = BigDecimal.ZERO;

    @Column(name = "current_stock")
    private BigDecimal currentStock = BigDecimal.ZERO;

    @Column(name = "is_enabled")
    private boolean enabled = true;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BomItem> bomItems = new ArrayList<>();

    @OneToMany(mappedBy = "material", cascade = CascadeType.ALL)
    private List<BomItem> usedInBoms = new ArrayList<>();

    public enum MaterialType {
        MATERIAL,
        SEMI_FINISHED,
        FINISHED_PRODUCT
    }

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
        if (minStock == null) {
            minStock = BigDecimal.ZERO;
        }
        if (maxStock == null) {
            maxStock = BigDecimal.ZERO;
        }
        if (currentStock == null) {
            currentStock = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
