package com.mes.dto;

import com.mes.entity.BomItem;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BomItemDTO {
    private Long id;
    private Long productId;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String materialUnit;
    private BigDecimal quantity;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static BomItemDTO fromEntity(BomItem item) {
        BomItemDTO dto = new BomItemDTO();
        dto.setId(item.getId());
        dto.setProductId(item.getProductId());
        dto.setMaterialId(item.getMaterialId());
        dto.setQuantity(item.getQuantity());
        dto.setRemark(item.getRemark());
        dto.setCreateTime(item.getCreateTime());
        dto.setUpdateTime(item.getUpdateTime());
        return dto;
    }

    public BomItem toEntity() {
        BomItem item = new BomItem();
        item.setId(this.id);
        item.setProductId(this.productId);
        item.setMaterialId(this.materialId);
        item.setQuantity(this.quantity);
        item.setRemark(this.remark);
        return item;
    }
}
