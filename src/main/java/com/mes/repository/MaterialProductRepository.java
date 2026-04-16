package com.mes.repository;

import com.mes.entity.MaterialProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaterialProductRepository extends JpaRepository<MaterialProduct, Long>, JpaSpecificationExecutor<MaterialProduct> {
    Optional<MaterialProduct> findByCode(String code);
    
    List<MaterialProduct> findByNameContaining(String name);
    
    boolean existsByCode(String code);
    
    List<MaterialProduct> findByCategoryId(Long categoryId);
    
    List<MaterialProduct> findByEnabled(boolean enabled);
    
    @Query("SELECT mp FROM MaterialProduct mp WHERE mp.categoryId IN :categoryIds")
    List<MaterialProduct> findByCategoryIds(@Param("categoryIds") List<Long> categoryIds);
    
    @Query("SELECT MAX(m.code) FROM MaterialProduct m WHERE m.code LIKE :prefix")
    String findMaxCodeByPrefix(@Param("prefix") String prefix);
}
