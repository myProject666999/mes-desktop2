package com.mes.service;

import com.mes.entity.Bom;
import com.mes.repository.BomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BomService {

    private final BomRepository bomRepository;

    public BomService(BomRepository bomRepository) {
        this.bomRepository = bomRepository;
    }

    public List<Bom> findAll() {
        return bomRepository.findAll();
    }

    public List<Bom> findByParentMaterialId(Long parentMaterialId) {
        return bomRepository.findByParentMaterialId(parentMaterialId);
    }

    public Bom findById(Long id) {
        return bomRepository.findById(id).orElse(null);
    }

    @Transactional
    public Bom create(Bom bom) {
        if (bomRepository.existsByParentMaterialIdAndChildMaterialId(
                bom.getParentMaterialId(), bom.getChildMaterialId())) {
            throw new RuntimeException("该BOM组成已存在");
        }
        return bomRepository.save(bom);
    }

    @Transactional
    public Bom update(Bom bom) {
        Bom existing = bomRepository.findById(bom.getId())
                .orElseThrow(() -> new RuntimeException("BOM组成不存在"));

        if (!existing.getChildMaterialId().equals(bom.getChildMaterialId()) &&
                bomRepository.existsByParentMaterialIdAndChildMaterialId(
                        bom.getParentMaterialId(), bom.getChildMaterialId())) {
            throw new RuntimeException("该BOM组成已存在");
        }

        existing.setChildMaterialId(bom.getChildMaterialId());
        existing.setQuantity(bom.getQuantity());
        existing.setRemarks(bom.getRemarks());
        return bomRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        bomRepository.deleteById(id);
    }

    @Transactional
    public void deleteByParentMaterialId(Long parentMaterialId) {
        bomRepository.deleteByParentMaterialId(parentMaterialId);
    }
}
