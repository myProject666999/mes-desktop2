package com.mes.service;

import com.mes.entity.MaterialCategory;
import com.mes.repository.MaterialCategoryRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MaterialCategoryService {

    private final MaterialCategoryRepository materialCategoryRepository;

    public MaterialCategoryService(MaterialCategoryRepository materialCategoryRepository) {
        this.materialCategoryRepository = materialCategoryRepository;
    }

    public List<MaterialCategory> findAll() {
        return materialCategoryRepository.findAll();
    }

    public List<MaterialCategory> search(String name, Boolean enabled) {
        Specification<MaterialCategory> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (name != null && !name.trim().isEmpty()) {
                predicates.add(cb.like(root.get("name"), "%" + name.trim() + "%"));
            }
            if (enabled != null) {
                predicates.add(cb.equal(root.get("enabled"), enabled));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return materialCategoryRepository.findAll(spec);
    }

    public List<MaterialCategory> findByParentId(Long parentId) {
        return materialCategoryRepository.findByParentId(parentId);
    }

    public List<MaterialCategory> buildTree() {
        List<MaterialCategory> allCategories = materialCategoryRepository.findAll();
        return buildTreeRecursive(null, allCategories);
    }

    private List<MaterialCategory> buildTreeRecursive(Long parentId, List<MaterialCategory> allCategories) {
        return allCategories.stream()
                .filter(category -> {
                    if (parentId == null) {
                        return category.getParentId() == null;
                    }
                    return parentId.equals(category.getParentId());
                })
                .peek(category -> category.setChildren(buildTreeRecursive(category.getId(), allCategories)))
                .collect(Collectors.toList());
    }

    public List<Long> getAllChildIds(Long parentId) {
        List<Long> result = new ArrayList<>();
        result.add(parentId);
        collectChildIds(parentId, result);
        return result;
    }

    private void collectChildIds(Long parentId, List<Long> result) {
        List<MaterialCategory> children = materialCategoryRepository.findByParentId(parentId);
        for (MaterialCategory child : children) {
            result.add(child.getId());
            collectChildIds(child.getId(), result);
        }
    }

    public MaterialCategory findById(Long id) {
        return materialCategoryRepository.findById(id).orElse(null);
    }

    @Transactional
    public MaterialCategory create(MaterialCategory category) {
        if (materialCategoryRepository.existsByName(category.getName())) {
            throw new RuntimeException("分类名称已存在: " + category.getName());
        }
        return materialCategoryRepository.save(category);
    }

    @Transactional
    public MaterialCategory update(MaterialCategory category) {
        MaterialCategory existing = materialCategoryRepository.findById(category.getId())
                .orElseThrow(() -> new RuntimeException("物料分类不存在"));

        if (!existing.getName().equals(category.getName()) &&
                materialCategoryRepository.existsByName(category.getName())) {
            throw new RuntimeException("分类名称已存在: " + category.getName());
        }

        existing.setName(category.getName());
        existing.setDescription(category.getDescription());
        existing.setEnabled(category.isEnabled());
        existing.setParentId(category.getParentId());
        return materialCategoryRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (materialCategoryRepository.existsByParentId(id)) {
            throw new RuntimeException("该分类下存在子分类，无法删除");
        }
        materialCategoryRepository.deleteById(id);
    }

    @Transactional
    public void deleteByIds(List<Long> ids) {
        for (Long id : ids) {
            delete(id);
        }
    }
}
