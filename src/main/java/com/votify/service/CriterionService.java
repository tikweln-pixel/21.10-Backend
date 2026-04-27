package com.votify.service;

import com.votify.dto.CriterionDto;
import com.votify.entity.Criterion;
import com.votify.persistence.CategoryCriterionPointsRepository;
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

    public CriterionService(CriterionRepository criterionRepository,
                            CategoryCriterionPointsRepository criterionPointsRepository,
                            VotingRepository votingRepository,
                            EvaluacionRepository evaluacionRepository) {
        this.criterionRepository = criterionRepository;
        this.criterionPointsRepository = criterionPointsRepository;
        this.votingRepository = votingRepository;
        this.evaluacionRepository = evaluacionRepository;
    }

    public List<CriterionDto> findAll() {
        List<Criterion> criteria = criterionRepository.findAll();
        List<CriterionDto> result = new ArrayList<>();
        for (Criterion criterion : criteria) {
            result.add(toDto(criterion));
        }
        return result;
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

    @Transactional
    public void delete(Long id) {
        if (id == null) throw new RuntimeException("Criterion ID cannot be null");
        if (!criterionRepository.existsById(id)) {
            throw new RuntimeException("Criterion not found with id: " + id);
        }
        votingRepository.deleteByCriterionId(id);
        evaluacionRepository.deleteByCriterionId(id);
        criterionPointsRepository.deleteByCriterionId(id);
        criterionRepository.deleteById(Objects.requireNonNull(id));
    }

    private CriterionDto toDto(Criterion criterion) {
        return new CriterionDto(criterion.getId(), criterion.getName());
    }
}
