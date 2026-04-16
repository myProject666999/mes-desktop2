package com.mes.repository;

import com.mes.entity.MaterialCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaterialCategoryRepository extends JpaRepository<MaterialCategory, Long>, JpaSpecificationExecutor<MaterialCategory> {
    Optional<MaterialCategory> findByCode(String code);
    
    List<MaterialCategory> findByNameContaining(String name);
    
    boolean existsByCode(String code);
    
    List<MaterialCategory> findByParentId(Long parentId);
    
    List<MaterialCategory> findByParentIdIsNull();
    
    List<MaterialCategory> findByEnabled(boolean enabled);
    
    List<MaterialCategory> findByParentIdAndEnabled(Long parentId, boolean enabled);
}
