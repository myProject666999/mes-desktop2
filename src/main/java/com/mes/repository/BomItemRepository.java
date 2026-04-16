package com.mes.repository;

import com.mes.entity.BomItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BomItemRepository extends JpaRepository<BomItem, Long>, JpaSpecificationExecutor<BomItem> {
    List<BomItem> findByProductId(Long productId);
    
    List<BomItem> findByMaterialId(Long materialId);
    
    void deleteByProductId(Long productId);
    
    boolean existsByMaterialId(Long materialId);
}
