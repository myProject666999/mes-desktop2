package com.mes.repository;

import com.mes.entity.Bom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BomRepository extends JpaRepository<Bom, Long>, JpaSpecificationExecutor<Bom> {
    List<Bom> findByParentMaterialId(Long parentMaterialId);

    Optional<Bom> findByParentMaterialIdAndChildMaterialId(Long parentMaterialId, Long childMaterialId);

    boolean existsByParentMaterialIdAndChildMaterialId(Long parentMaterialId, Long childMaterialId);

    void deleteByParentMaterialId(Long parentMaterialId);
}
