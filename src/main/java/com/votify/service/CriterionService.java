package com.votify.service;

import com.votify.dto.CriterionDto;
import com.votify.entity.Criterion;
import com.votify.persistence.CriterionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CriterionService {

    private final CriterionRepository criterionRepository;

    public CriterionService(CriterionRepository criterionRepository) {
        this.criterionRepository = criterionRepository;
    }

    public List<CriterionDto> findAll() {
        return criterionRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public CriterionDto findById(Long id) {
        Criterion criterion = criterionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Criterion not found with id: " + id));
        return toDto(criterion);
    }

    public CriterionDto create(CriterionDto dto) {
        Criterion criterion = new Criterion(dto.getName());
        return toDto(criterionRepository.save(criterion));
    }

    public CriterionDto update(Long id, CriterionDto dto) {
        Criterion criterion = criterionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Criterion not found with id: " + id));
        criterion.setName(dto.getName());
        return toDto(criterionRepository.save(criterion));
    }

    public void delete(Long id) {
        criterionRepository.deleteById(id);
    }

    private CriterionDto toDto(Criterion criterion) {
        return new CriterionDto(criterion.getId(), criterion.getName());
    }
}
