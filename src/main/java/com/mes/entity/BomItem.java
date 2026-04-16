package com.mes.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "bom_item")
@EqualsAndHashCode(exclude = {"product", "material"})
@ToString(exclude = {"product", "material"})
public class BomItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private MaterialProduct product;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", insertable = false, updatable = false)
    private MaterialProduct material;

    @Column(nullable = false)
    private BigDecimal quantity = BigDecimal.ONE;

    private String remark;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
        if (quantity == null) {
            quantity = BigDecimal.ONE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
