package com.votify.service;

import com.votify.dto.CriterionDto;
import com.votify.entity.Criterion;
import com.votify.persistence.CriterionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
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
        if (id == null) throw new RuntimeException("Criterion ID cannot be null");
        Criterion criterion = criterionRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("Criterion not found with id: " + id));
        return toDto(criterion);
    }

    public CriterionDto create(CriterionDto dto) {
        Criterion criterion = new Criterion(dto.getName());
        return toDto(criterionRepository.save(Objects.requireNonNull(criterion)));
    }

    public CriterionDto update(Long id, CriterionDto dto) {
        if (id == null) throw new RuntimeException("Criterion ID cannot be null");
        Criterion criterion = criterionRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("Criterion not found with id: " + id));
        criterion.setName(dto.getName());
        return toDto(criterionRepository.save(Objects.requireNonNull(criterion)));
    }

    public void delete(Long id) {
        if (id == null) throw new RuntimeException("Criterion ID cannot be null");
        criterionRepository.deleteById(Objects.requireNonNull(id));
    }

    private CriterionDto toDto(Criterion criterion) {
        return new CriterionDto(criterion.getId(), criterion.getName());
    }
}
