package com.mes.repository;

import com.mes.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Long>, JpaSpecificationExecutor<Material> {
    Optional<Material> findByCode(String code);

    List<Material> findByCategoryId(Long categoryId);

    @Query("SELECT m FROM Material m WHERE m.categoryId IN :categoryIds")
    List<Material> findByCategoryIdIn(List<Long> categoryIds);

    List<Material> findByNameContaining(String name);

    boolean existsByCode(String code);

    @Query("SELECT MAX(m.code) FROM Material m WHERE m.code LIKE :prefix%")
    String findMaxCodeByPrefix(String prefix);
}
