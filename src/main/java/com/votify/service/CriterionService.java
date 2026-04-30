package com.votify.service;

import com.votify.dto.CriterionDto;
import com.votify.entity.Category;
import com.votify.entity.Criterion;
import com.votify.persistence.CategoryCriterionPointsRepository;
import com.votify.persistence.CategoryRepository;
import com.votify.application.strategy.VoteWeightingStrategy;
import org.springframework.beans.factory.annotation.Qualifier;
import com.votify.persistence.CriterionRepository;
import com.votify.persistence.EvaluacionRepository;
import com.votify.persistence.VotingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class CriterionService {

    private final CriterionRepository criterionRepository;
    private final CategoryCriterionPointsRepository criterionPointsRepository;
    private final VotingRepository votingRepository;
    private final EvaluacionRepository evaluacionRepository;
    private final CategoryRepository categoryRepository;
    private final java.util.Map<String, VoteWeightingStrategy> weightingStrategies;

    public CriterionService(CriterionRepository criterionRepository,
                            CategoryCriterionPointsRepository criterionPointsRepository,
                            VotingRepository votingRepository,
                            EvaluacionRepository evaluacionRepository,
                            CategoryRepository categoryRepository,
                            java.util.Map<String, VoteWeightingStrategy> weightingStrategies) {
        this.criterionRepository = criterionRepository;
        this.criterionPointsRepository = criterionPointsRepository;
        this.votingRepository = votingRepository;
        this.evaluacionRepository = evaluacionRepository;
        this.categoryRepository = categoryRepository;
        this.weightingStrategies = weightingStrategies;
    }

    public List<CriterionDto> findAll() {
        List<Criterion> criteria = criterionRepository.findAll();
        List<CriterionDto> result = new ArrayList<>();
        for (Criterion criterion : criteria) {
            result.add(toDto(criterion));
        }
        return result;
    }

    // Helper público: construye mapa por key() a partir de beans inyectados
    public java.util.Map<String, VoteWeightingStrategy> getStrategiesByKey() {
        java.util.Map<String, VoteWeightingStrategy> byKey = new java.util.HashMap<>();
        if (weightingStrategies != null) {
            for (VoteWeightingStrategy s : weightingStrategies.values()) {
                try {
                    byKey.put(s.key(), s);
                } catch (Exception ex) {
                    // ignore si una implementación falla en key()
                }
            }
        }
        return byKey;
    }

    // Selecciona una estrategia en base a la categoría. Lógica mejorada considerando weightingPolicy.
    public VoteWeightingStrategy getStrategyForCategory(Category category) {
        java.util.Map<String, VoteWeightingStrategy> byKey = getStrategiesByKey();
        if (byKey.isEmpty()) return null;

        if (category != null) {
            // 1. Si la categoría define explícitamente una política, usarla
            String policy = category.getWeightingPolicy();
            if (policy != null && !policy.isEmpty()) {
                VoteWeightingStrategy strategy = byKey.get(policy);
                if (strategy != null) {
                    return strategy;
                }
            }

            // 2. Fallback: si tiene totalPoints definido, preferir categoryFactor
            if (category.getTotalPoints() != null && category.getTotalPoints() > 0) {
                return byKey.getOrDefault("categoryFactor", byKey.get("default"));
            }
        }

        // 3. Fallback final: estrategia por defecto
        return byKey.getOrDefault("default", byKey.values().iterator().next());
    }

    public List<CriterionDto> findByCategoryId(Long categoryId) {
        List<Criterion> criteria = criterionRepository.findByCategoryId(categoryId);
        List<CriterionDto> result = new ArrayList<>();
        for (Criterion criterion : criteria) {
            result.add(toDto(criterion));
        }
        return result;
    }

    public CriterionDto findById(Long id) {
        if (id == null) throw new RuntimeException("El ID del criterio no puede ser nulo");
        Criterion criterion = criterionRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("Criterion not found with id: " + id));
        return toDto(criterion);
    }

    public CriterionDto create(CriterionDto dto) {
        if (dto.getCategoryId() == null) {
            throw new RuntimeException("Se requiere una categoría para crear un criterio");
        }
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con id: " + dto.getCategoryId()));
        Criterion criterion = new Criterion(dto.getName());
        criterion.setCategory(category);
        return toDto(criterionRepository.save(criterion));
    }

    public CriterionDto update(Long id, CriterionDto dto) {
        if (id == null) throw new RuntimeException("El ID del criterio no puede ser nulo");
        Criterion criterion = criterionRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("Criterion not found with id: " + id));
        criterion.setName(dto.getName());
        return toDto(criterionRepository.save(Objects.requireNonNull(criterion)));
    }

    @Transactional
    public void delete(Long id) {
        if (id == null) throw new RuntimeException("El ID del criterio no puede ser nulo");
        if (!criterionRepository.existsById(id)) {
            throw new RuntimeException("Criterio no encontrado con id: " + id);
        }
        votingRepository.deleteByCriterionId(id);
        evaluacionRepository.deleteByCriterionId(id);
        criterionPointsRepository.deleteByCriterionId(id);
        criterionRepository.deleteById(Objects.requireNonNull(id));
    }

    private CriterionDto toDto(Criterion criterion) {
        Long categoryId = criterion.getCategory() != null ? criterion.getCategory().getId() : null;
        return new CriterionDto(criterion.getId(), criterion.getName(), categoryId);
    }
}

