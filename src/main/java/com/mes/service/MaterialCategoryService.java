package com.mes.service;

import com.mes.dto.MaterialCategoryDTO;
import com.mes.entity.MaterialCategory;
import com.mes.repository.MaterialCategoryRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MaterialCategoryService {

    private final MaterialCategoryRepository categoryRepository;

    public MaterialCategoryService(MaterialCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<MaterialCategory> findAll() {
        return categoryRepository.findAll();
    }

    public List<MaterialCategoryDTO> findAllTree() {
        List<MaterialCategory> allCategories = categoryRepository.findAll();
        return buildTree(allCategories);
    }

    public List<MaterialCategoryDTO> findAllEnabledTree() {
        List<MaterialCategory> allCategories = categoryRepository.findByEnabled(true);
        return buildTree(allCategories);
    }

    private List<MaterialCategoryDTO> buildTree(List<MaterialCategory> allCategories) {
        Map<Long, MaterialCategoryDTO> dtoMap = allCategories.stream()
                .collect(Collectors.toMap(MaterialCategory::getId, MaterialCategoryDTO::fromEntity));

        List<MaterialCategoryDTO> roots = new ArrayList<>();
        
        for (MaterialCategory category : allCategories) {
            MaterialCategoryDTO dto = dtoMap.get(category.getId());
            if (category.getParentId() == null) {
                roots.add(dto);
            } else {
                MaterialCategoryDTO parentDto = dtoMap.get(category.getParentId());
                if (parentDto != null) {
                    parentDto.getChildren().add(dto);
                }
            }
        }

        roots.sort((a, b) -> {
            int orderA = a.getSortOrder() != null ? a.getSortOrder() : 0;
            int orderB = b.getSortOrder() != null ? b.getSortOrder() : 0;
            return Integer.compare(orderA, orderB);
        });

        return roots;
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
        return categoryRepository.findAll(spec);
    }

    public List<MaterialCategoryDTO> searchTree(String name, Boolean enabled) {
        List<MaterialCategory> categories = search(name, enabled);
        return buildTree(categories);
    }

    public MaterialCategory findById(Long id) {
        return categoryRepository.findById(id).orElse(null);
    }

    public List<MaterialCategory> findByParentId(Long parentId) {
        return categoryRepository.findByParentId(parentId);
    }

    public List<MaterialCategory> findRootCategories() {
        return categoryRepository.findByParentIdIsNull();
    }

    @Transactional
    public MaterialCategory create(MaterialCategory category) {
        if (categoryRepository.existsByCode(category.getCode())) {
            throw new RuntimeException("分类编码已存在: " + category.getCode());
        }
        if (category.getParentId() != null) {
            MaterialCategory parent = categoryRepository.findById(category.getParentId())
                    .orElseThrow(() -> new RuntimeException("父级分类不存在"));
        }
        return categoryRepository.save(category);
    }

    @Transactional
    public MaterialCategory update(MaterialCategory category) {
        MaterialCategory existing = categoryRepository.findById(category.getId())
                .orElseThrow(() -> new RuntimeException("分类不存在"));
        
        if (!existing.getCode().equals(category.getCode()) && 
            categoryRepository.existsByCode(category.getCode())) {
            throw new RuntimeException("分类编码已存在: " + category.getCode());
        }

        if (category.getParentId() != null && category.getParentId().equals(category.getId())) {
            throw new RuntimeException("不能将自己设置为父级分类");
        }
        
        existing.setCode(category.getCode());
        existing.setName(category.getName());
        existing.setParentId(category.getParentId());
        existing.setDescription(category.getDescription());
        existing.setSortOrder(category.getSortOrder());
        existing.setEnabled(category.isEnabled());
        return categoryRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        List<MaterialCategory> children = categoryRepository.findByParentId(id);
        if (!children.isEmpty()) {
            throw new RuntimeException("该分类下存在子分类，无法删除");
        }
        categoryRepository.deleteById(id);
    }

    @Transactional
    public void deleteByIds(List<Long> ids) {
        for (Long id : ids) {
            delete(id);
        }
    }

    public List<Long> getAllChildCategoryIds(Long categoryId) {
        List<Long> ids = new ArrayList<>();
        ids.add(categoryId);
        collectChildIds(categoryId, ids);
        return ids;
    }

    private void collectChildIds(Long parentId, List<Long> ids) {
        List<MaterialCategory> children = categoryRepository.findByParentId(parentId);
        for (MaterialCategory child : children) {
            ids.add(child.getId());
            collectChildIds(child.getId(), ids);
        }
    }
}
