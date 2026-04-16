package com.mes.service;

import com.mes.dto.BomItemDTO;
import com.mes.dto.MaterialProductDTO;
import com.mes.entity.BomItem;
import com.mes.entity.MaterialCategory;
import com.mes.entity.MaterialProduct;
import com.mes.repository.BomItemRepository;
import com.mes.repository.MaterialProductRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MaterialProductService {

    private final MaterialProductRepository productRepository;
    private final BomItemRepository bomItemRepository;
    private final MaterialCategoryService categoryService;

    public MaterialProductService(MaterialProductRepository productRepository,
                                  BomItemRepository bomItemRepository,
                                  MaterialCategoryService categoryService) {
        this.productRepository = productRepository;
        this.bomItemRepository = bomItemRepository;
        this.categoryService = categoryService;
    }

    public List<MaterialProduct> findAll() {
        return productRepository.findAll();
    }

    public List<MaterialProductDTO> findAllDTO() {
        List<MaterialProduct> products = productRepository.findAll();
        return convertToDTOList(products);
    }

    public List<MaterialProduct> findByCategoryId(Long categoryId) {
        List<Long> categoryIds = categoryService.getAllChildCategoryIds(categoryId);
        return productRepository.findByCategoryIds(categoryIds);
    }

    public List<MaterialProductDTO> findByCategoryIdDTO(Long categoryId) {
        if (categoryId == null) {
            return findAllDTO();
        }
        List<MaterialProduct> products = findByCategoryId(categoryId);
        return convertToDTOList(products);
    }

    public List<MaterialProduct> search(String code, String name, Long categoryId, Boolean enabled) {
        Specification<MaterialProduct> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (code != null && !code.trim().isEmpty()) {
                predicates.add(cb.like(root.get("code"), "%" + code.trim() + "%"));
            }
            if (name != null && !name.trim().isEmpty()) {
                predicates.add(cb.like(root.get("name"), "%" + name.trim() + "%"));
            }
            if (categoryId != null) {
                List<Long> categoryIds = categoryService.getAllChildCategoryIds(categoryId);
                predicates.add(root.get("categoryId").in(categoryIds));
            }
            if (enabled != null) {
                predicates.add(cb.equal(root.get("enabled"), enabled));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return productRepository.findAll(spec);
    }

    public List<MaterialProductDTO> searchDTO(String code, String name, Long categoryId, Boolean enabled) {
        List<MaterialProduct> products = search(code, name, categoryId, enabled);
        return convertToDTOList(products);
    }

    private List<MaterialProductDTO> convertToDTOList(List<MaterialProduct> products) {
        List<MaterialCategory> categories = categoryService.findAll();
        Map<Long, String> categoryNameMap = categories.stream()
                .collect(Collectors.toMap(MaterialCategory::getId, MaterialCategory::getName));

        return products.stream().map(product -> {
            MaterialProductDTO dto = MaterialProductDTO.fromEntity(product);
            if (product.getCategoryId() != null) {
                dto.setCategoryName(categoryNameMap.get(product.getCategoryId()));
            }
            return dto;
        }).collect(Collectors.toList());
    }

    public MaterialProduct findById(Long id) {
        return productRepository.findById(id).orElse(null);
    }

    public MaterialProductDTO findByIdDTO(Long id) {
        MaterialProduct product = findById(id);
        if (product == null) {
            return null;
        }
        
        MaterialProductDTO dto = MaterialProductDTO.fromEntity(product);
        
        if (product.getCategoryId() != null) {
            MaterialCategory category = categoryService.findById(product.getCategoryId());
            if (category != null) {
                dto.setCategoryName(category.getName());
            }
        }
        
        List<BomItem> bomItems = bomItemRepository.findByProductId(product.getId());
        if (!bomItems.isEmpty()) {
            List<Long> materialIds = bomItems.stream()
                    .map(BomItem::getMaterialId)
                    .collect(Collectors.toList());
            List<MaterialProduct> materials = productRepository.findAllById(materialIds);
            Map<Long, MaterialProduct> materialMap = materials.stream()
                    .collect(Collectors.toMap(MaterialProduct::getId, m -> m));
            
            List<BomItemDTO> bomItemDTOs = bomItems.stream().map(item -> {
                BomItemDTO itemDTO = BomItemDTO.fromEntity(item);
                MaterialProduct material = materialMap.get(item.getMaterialId());
                if (material != null) {
                    itemDTO.setMaterialCode(material.getCode());
                    itemDTO.setMaterialName(material.getName());
                    itemDTO.setMaterialUnit(material.getUnit());
                }
                return itemDTO;
            }).collect(Collectors.toList());
            dto.setBomItems(bomItemDTOs);
        }
        
        return dto;
    }

    public String generateCode(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            prefix = "M";
        }
        String maxCode = productRepository.findMaxCodeByPrefix(prefix + "%");
        if (maxCode == null) {
            return prefix + "0001";
        }
        try {
            String numStr = maxCode.substring(prefix.length());
            int num = Integer.parseInt(numStr) + 1;
            return prefix + String.format("%04d", num);
        } catch (NumberFormatException e) {
            return prefix + "0001";
        }
    }

    @Transactional
    public MaterialProduct create(MaterialProduct product) {
        if (productRepository.existsByCode(product.getCode())) {
            throw new RuntimeException("物料编码已存在: " + product.getCode());
        }
        if (product.getCategoryId() != null) {
            if (categoryService.findById(product.getCategoryId()) == null) {
                throw new RuntimeException("物料分类不存在");
            }
        }
        return productRepository.save(product);
    }

    @Transactional
    public MaterialProduct update(MaterialProduct product) {
        MaterialProduct existing = productRepository.findById(product.getId())
                .orElseThrow(() -> new RuntimeException("物料不存在"));
        
        if (!existing.getCode().equals(product.getCode()) && 
            productRepository.existsByCode(product.getCode())) {
            throw new RuntimeException("物料编码已存在: " + product.getCode());
        }

        if (product.getCategoryId() != null) {
            if (categoryService.findById(product.getCategoryId()) == null) {
                throw new RuntimeException("物料分类不存在");
            }
        }
        
        existing.setCode(product.getCode());
        existing.setName(product.getName());
        existing.setCategoryId(product.getCategoryId());
        existing.setSpecification(product.getSpecification());
        existing.setModel(product.getModel());
        existing.setUnit(product.getUnit());
        existing.setDescription(product.getDescription());
        existing.setMaterialType(product.getMaterialType());
        existing.setMinStock(product.getMinStock());
        existing.setMaxStock(product.getMaxStock());
        existing.setCurrentStock(product.getCurrentStock());
        existing.setEnabled(product.isEnabled());
        return productRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (bomItemRepository.existsByMaterialId(id)) {
            throw new RuntimeException("该物料已被其他BOM引用，无法删除");
        }
        bomItemRepository.deleteByProductId(id);
        productRepository.deleteById(id);
    }

    @Transactional
    public void deleteByIds(List<Long> ids) {
        for (Long id : ids) {
            delete(id);
        }
    }

    @Transactional
    public List<BomItemDTO> getBomItems(Long productId) {
        List<BomItem> bomItems = bomItemRepository.findByProductId(productId);
        if (bomItems.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Long> materialIds = bomItems.stream()
                .map(BomItem::getMaterialId)
                .collect(Collectors.toList());
        List<MaterialProduct> materials = productRepository.findAllById(materialIds);
        Map<Long, MaterialProduct> materialMap = materials.stream()
                .collect(Collectors.toMap(MaterialProduct::getId, m -> m));
        
        return bomItems.stream().map(item -> {
            BomItemDTO dto = BomItemDTO.fromEntity(item);
            MaterialProduct material = materialMap.get(item.getMaterialId());
            if (material != null) {
                dto.setMaterialCode(material.getCode());
                dto.setMaterialName(material.getName());
                dto.setMaterialUnit(material.getUnit());
            }
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public BomItem addBomItem(Long productId, Long materialId, BigDecimal quantity, String remark) {
        MaterialProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("产品不存在"));
        MaterialProduct material = productRepository.findById(materialId)
                .orElseThrow(() -> new RuntimeException("物料不存在"));
        
        if (productId.equals(materialId)) {
            throw new RuntimeException("不能将自己添加为BOM组件");
        }

        List<BomItem> existingItems = bomItemRepository.findByProductId(productId);
        for (BomItem item : existingItems) {
            if (item.getMaterialId().equals(materialId)) {
                throw new RuntimeException("该物料已在BOM中存在");
            }
        }

        BomItem bomItem = new BomItem();
        bomItem.setProductId(productId);
        bomItem.setMaterialId(materialId);
        bomItem.setQuantity(quantity != null ? quantity : BigDecimal.ONE);
        bomItem.setRemark(remark);
        return bomItemRepository.save(bomItem);
    }

    @Transactional
    public BomItem updateBomItem(Long bomItemId, BigDecimal quantity, String remark) {
        BomItem item = bomItemRepository.findById(bomItemId)
                .orElseThrow(() -> new RuntimeException("BOM项不存在"));
        item.setQuantity(quantity != null ? quantity : BigDecimal.ONE);
        item.setRemark(remark);
        return bomItemRepository.save(item);
    }

    @Transactional
    public void deleteBomItem(Long bomItemId) {
        bomItemRepository.deleteById(bomItemId);
    }

    public List<MaterialProductDTO> findAvailableMaterialsForBom(Long productId) {
        List<MaterialProduct> allProducts = productRepository.findAll();
        return allProducts.stream()
                .filter(p -> !p.getId().equals(productId))
                .map(MaterialProductDTO::fromEntity)
                .collect(Collectors.toList());
    }
}
