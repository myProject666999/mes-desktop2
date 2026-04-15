package com.mes.dto;

import com.mes.entity.Material;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MaterialDTO {
    private Long id;
    private String code;
    private String name;
    private String specification;
    private String unit;
    private Long categoryId;
    private String categoryName;
    private BigDecimal minStock;
    private BigDecimal maxStock;
    private String remarks;
    private boolean enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static MaterialDTO fromEntity(Material material) {
        MaterialDTO dto = new MaterialDTO();
        dto.setId(material.getId());
        dto.setCode(material.getCode());
        dto.setName(material.getName());
        dto.setSpecification(material.getSpecification());
        dto.setUnit(material.getUnit());
        dto.setCategoryId(material.getCategoryId());
        if (material.getCategory() != null) {
            dto.setCategoryName(material.getCategory().getName());
        }
        dto.setMinStock(material.getMinStock());
        dto.setMaxStock(material.getMaxStock());
        dto.setRemarks(material.getRemarks());
        dto.setEnabled(material.isEnabled());
        dto.setCreateTime(material.getCreateTime());
        dto.setUpdateTime(material.getUpdateTime());
        return dto;
    }

    public Material toEntity() {
        Material material = new Material();
        material.setId(this.id);
        material.setCode(this.code);
        material.setName(this.name);
        material.setSpecification(this.specification);
        material.setUnit(this.unit);
        material.setCategoryId(this.categoryId);
        material.setMinStock(this.minStock);
        material.setMaxStock(this.maxStock);
        material.setRemarks(this.remarks);
        material.setEnabled(this.enabled);
        return material;
    }
}
