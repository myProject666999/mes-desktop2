package com.mes.dto;

import com.mes.entity.MaterialCategory;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class MaterialCategoryDTO {
    private Long id;
    private String code;
    private String name;
    private Long parentId;
    private String parentName;
    private String description;
    private Integer sortOrder;
    private boolean enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<MaterialCategoryDTO> children = new ArrayList<>();

    public static MaterialCategoryDTO fromEntity(MaterialCategory category) {
        MaterialCategoryDTO dto = new MaterialCategoryDTO();
        dto.setId(category.getId());
        dto.setCode(category.getCode());
        dto.setName(category.getName());
        dto.setParentId(category.getParentId());
        dto.setDescription(category.getDescription());
        dto.setSortOrder(category.getSortOrder());
        dto.setEnabled(category.isEnabled());
        dto.setCreateTime(category.getCreateTime());
        dto.setUpdateTime(category.getUpdateTime());
        dto.setChildren(new ArrayList<>());
        return dto;
    }

    public MaterialCategory toEntity() {
        MaterialCategory category = new MaterialCategory();
        category.setId(this.id);
        category.setCode(this.code);
        category.setName(this.name);
        category.setParentId(this.parentId);
        category.setDescription(this.description);
        category.setSortOrder(this.sortOrder);
        category.setEnabled(this.enabled);
        return category;
    }
}
