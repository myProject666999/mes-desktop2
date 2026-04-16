package com.mes.dto;

import com.mes.entity.MaterialProduct;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class MaterialProductDTO {
    private Long id;
    private String code;
    private String name;
    private Long categoryId;
    private String categoryName;
    private String specification;
    private String model;
    private String unit;
    private String description;
    private MaterialProduct.MaterialType materialType;
    private BigDecimal minStock;
    private BigDecimal maxStock;
    private BigDecimal currentStock;
    private boolean enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<BomItemDTO> bomItems = new ArrayList<>();

    public static MaterialProductDTO fromEntity(MaterialProduct product) {
        MaterialProductDTO dto = new MaterialProductDTO();
        dto.setId(product.getId());
        dto.setCode(product.getCode());
        dto.setName(product.getName());
        dto.setCategoryId(product.getCategoryId());
        dto.setSpecification(product.getSpecification());
        dto.setModel(product.getModel());
        dto.setUnit(product.getUnit());
        dto.setDescription(product.getDescription());
        dto.setMaterialType(product.getMaterialType());
        dto.setMinStock(product.getMinStock());
        dto.setMaxStock(product.getMaxStock());
        dto.setCurrentStock(product.getCurrentStock());
        dto.setEnabled(product.isEnabled());
        dto.setCreateTime(product.getCreateTime());
        dto.setUpdateTime(product.getUpdateTime());
        dto.setBomItems(new ArrayList<>());
        return dto;
    }

    public MaterialProduct toEntity() {
        MaterialProduct product = new MaterialProduct();
        product.setId(this.id);
        product.setCode(this.code);
        product.setName(this.name);
        product.setCategoryId(this.categoryId);
        product.setSpecification(this.specification);
        product.setModel(this.model);
        product.setUnit(this.unit);
        product.setDescription(this.description);
        product.setMaterialType(this.materialType);
        product.setMinStock(this.minStock);
        product.setMaxStock(this.maxStock);
        product.setCurrentStock(this.currentStock);
        product.setEnabled(this.enabled);
        return product;
    }
}
