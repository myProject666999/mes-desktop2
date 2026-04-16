package com.mes.dto;

import com.mes.entity.MaterialCategory;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MaterialCategoryDTO {
    private Long id;
    private String name;
    private String description;
    private boolean enabled;
    private Long parentId;
    private String parentName;
    private List<MaterialCategoryDTO> children;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static MaterialCategoryDTO fromEntity(MaterialCategory category) {
        MaterialCategoryDTO dto = new MaterialCategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setEnabled(category.isEnabled());
        dto.setParentId(category.getParentId());
        dto.setCreateTime(category.getCreateTime());
        dto.setUpdateTime(category.getUpdateTime());
        return dto;
    }

    public MaterialCategory toEntity() {
        MaterialCategory category = new MaterialCategory();
        category.setId(this.id);
        category.setName(this.name);
        category.setDescription(this.description);
        category.setEnabled(this.enabled);
        category.setParentId(this.parentId);
        return category;
    }
}
