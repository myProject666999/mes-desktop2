package com.mes.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "bom")
@EqualsAndHashCode
@ToString
public class Bom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_material_id", nullable = false)
    private Long parentMaterialId;

    @ManyToOne
    @JoinColumn(name = "parent_material_id", insertable = false, updatable = false)
    private Material parentMaterial;

    @Column(name = "child_material_id", nullable = false)
    private Long childMaterialId;

    @ManyToOne
    @JoinColumn(name = "child_material_id", insertable = false, updatable = false)
    private Material childMaterial;

    @Column(nullable = false)
    private BigDecimal quantity = BigDecimal.ONE;

    private String remarks;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
