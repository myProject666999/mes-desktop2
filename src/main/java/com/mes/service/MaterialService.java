package com.mes.service;

import com.mes.entity.Material;
import com.mes.repository.MaterialRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
public class MaterialService {

    private final MaterialRepository materialRepository;

    public MaterialService(MaterialRepository materialRepository) {
        this.materialRepository = materialRepository;
    }

    public List<Material> findAll() {
        return materialRepository.findAll();
    }

    public List<Material> search(String code, String name, List<Long> categoryIds) {
        Specification<Material> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (code != null && !code.trim().isEmpty()) {
                predicates.add(cb.like(root.get("code"), "%" + code.trim() + "%"));
            }
            if (name != null && !name.trim().isEmpty()) {
                predicates.add(cb.like(root.get("name"), "%" + name.trim() + "%"));
            }
            if (categoryIds != null && !categoryIds.isEmpty()) {
                predicates.add(root.get("categoryId").in(categoryIds));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return materialRepository.findAll(spec);
    }

    public List<Material> findByCategoryId(Long categoryId) {
        return materialRepository.findByCategoryId(categoryId);
    }

    public List<Material> findByCategoryIds(List<Long> categoryIds) {
        return materialRepository.findByCategoryIdIn(categoryIds);
    }

    public Material findById(Long id) {
        return materialRepository.findById(id).orElse(null);
    }

    public Material findByCode(String code) {
        return materialRepository.findByCode(code).orElse(null);
    }

    public String generateCode(String prefix) {
        String actualPrefix = (prefix == null || prefix.trim().isEmpty()) ? "MAT" : prefix.trim().toUpperCase();
        String maxCode = materialRepository.findMaxCodeByPrefix(actualPrefix);
        int sequence = 1;
        if (maxCode != null) {
            try {
                String numPart = maxCode.substring(actualPrefix.length());
                sequence = Integer.parseInt(numPart) + 1;
            } catch (Exception e) {
                sequence = 1;
            }
        }
        return actualPrefix + String.format("%06d", sequence);
    }

    @Transactional
    public Material create(Material material) {
        if (materialRepository.existsByCode(material.getCode())) {
            throw new RuntimeException("物料编码已存在: " + material.getCode());
        }
        return materialRepository.save(material);
    }

    @Transactional
    public Material update(Material material) {
        Material existing = materialRepository.findById(material.getId())
                .orElseThrow(() -> new RuntimeException("物料不存在"));

        if (!existing.getCode().equals(material.getCode()) &&
                materialRepository.existsByCode(material.getCode())) {
            throw new RuntimeException("物料编码已存在: " + material.getCode());
        }

        existing.setCode(material.getCode());
        existing.setName(material.getName());
        existing.setSpecification(material.getSpecification());
        existing.setUnit(material.getUnit());
        existing.setCategoryId(material.getCategoryId());
        existing.setMinStock(material.getMinStock());
        existing.setMaxStock(material.getMaxStock());
        existing.setRemarks(material.getRemarks());
        existing.setEnabled(material.isEnabled());
        return materialRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        materialRepository.deleteById(id);
    }

    @Transactional
    public void deleteByIds(List<Long> ids) {
        materialRepository.deleteAllById(ids);
    }
}
