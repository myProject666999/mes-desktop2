package com.mes.repository;

import com.mes.entity.MaterialCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialCategoryRepository extends JpaRepository<MaterialCategory, Long>, JpaSpecificationExecutor<MaterialCategory> {
    List<MaterialCategory> findByParentId(Long parentId);

    List<MaterialCategory> findByNameContainingAndEnabled(String name, Boolean enabled);

    List<MaterialCategory> findByNameContaining(String name);

    List<MaterialCategory> findByEnabled(Boolean enabled);

    boolean existsByName(String name);

    boolean existsByParentId(Long parentId);
}
