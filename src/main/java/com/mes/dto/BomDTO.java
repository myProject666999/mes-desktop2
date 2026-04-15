package com.mes.dto;

import com.mes.entity.Bom;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BomDTO {
    private Long id;
    private Long parentMaterialId;
    private String parentMaterialCode;
    private String parentMaterialName;
    private Long childMaterialId;
    private String childMaterialCode;
    private String childMaterialName;
    private BigDecimal quantity;
    private String remarks;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static BomDTO fromEntity(Bom bom) {
        BomDTO dto = new BomDTO();
        dto.setId(bom.getId());
        dto.setParentMaterialId(bom.getParentMaterialId());
        dto.setChildMaterialId(bom.getChildMaterialId());
        dto.setQuantity(bom.getQuantity());
        dto.setRemarks(bom.getRemarks());
        dto.setCreateTime(bom.getCreateTime());
        dto.setUpdateTime(bom.getUpdateTime());
        if (bom.getParentMaterial() != null) {
            dto.setParentMaterialCode(bom.getParentMaterial().getCode());
            dto.setParentMaterialName(bom.getParentMaterial().getName());
        }
        if (bom.getChildMaterial() != null) {
            dto.setChildMaterialCode(bom.getChildMaterial().getCode());
            dto.setChildMaterialName(bom.getChildMaterial().getName());
        }
        return dto;
    }

    public Bom toEntity() {
        Bom bom = new Bom();
        bom.setId(this.id);
        bom.setParentMaterialId(this.parentMaterialId);
        bom.setChildMaterialId(this.childMaterialId);
        bom.setQuantity(this.quantity);
        bom.setRemarks(this.remarks);
        return bom;
    }
}
