package com.votify.service;

import com.votify.dto.ProjectRankingDto;
import com.votify.dto.VotingDto;
import com.votify.entity.*;
import com.votify.exception.EntityNotFoundException;
import com.votify.exception.ValidationException;
import com.votify.mapper.VotingMapper;
import com.votify.persistence.*;
import com.votify.service.observer.VotoObserver;
import com.votify.service.observer.VotoSubject;
import com.votify.validator.EntityValidator;
import com.votify.validator.VotingValidator;
import com.votify.validator.VotingValidatorFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


@Service
public class VotingService implements VotoSubject {

    private final List<VotoObserver> observers = new ArrayList<>();

    private final VotingRepository votingRepository;
    private final UserRepository userRepository;
    private final CriterionRepository criterionRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryCriterionPointsRepository criterionPointsRepository;
    private final ProjectRepository projectRepository;
    private final CriterionService criterionService;
    private final EntityValidator entityValidator;
    private final VotingValidatorFactory votingValidatorFactory;
    private final VotingMapper votingMapper;

    public VotingService(VotingRepository votingRepository,
                         UserRepository userRepository,
                         CriterionRepository criterionRepository,
                         CategoryRepository categoryRepository,
                         CategoryCriterionPointsRepository criterionPointsRepository,
                         ProjectRepository projectRepository,
                         CriterionService criterionService,
                         EntityValidator entityValidator,
                         VotingValidatorFactory votingValidatorFactory,
                         VotingMapper votingMapper) {
        this.votingRepository = votingRepository;
        this.userRepository = userRepository;
        this.criterionRepository = criterionRepository;
        this.categoryRepository = categoryRepository;
        this.criterionPointsRepository = criterionPointsRepository;
        this.projectRepository = projectRepository;
        this.criterionService = criterionService;
        this.entityValidator = entityValidator;
        this.votingValidatorFactory = votingValidatorFactory;
        this.votingMapper = votingMapper;
    }

    @Override
    public void addObserver(VotoObserver observer) { observers.add(observer); }

    @Override
    public void removeObserver(VotoObserver observer) { observers.remove(observer); }

    @Override
    public void notifyObservers(Voting voting) { observers.forEach(o -> o.onVotoGuardado(voting)); }

    public List<VotingDto> findAll() {
        return votingRepository.findAll()
                .stream()
                .map(votingMapper::toDto)
                .collect(Collectors.toList());
    }

    public VotingDto findById(Long id) {
        Voting voting = votingRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new EntityNotFoundException("Voting", id));
        return votingMapper.toDto(voting);
    }

    public VotingDto create(VotingDto dto) {
        // Cargar y validar entidades
        User voter = entityValidator.getUserOrThrow(dto.getVoterId());
        Project project = entityValidator.getProjectOrThrow(dto.getProjectId());
        Criterion criterion = entityValidator.getCriterionOrThrow(dto.getCriterionId());

        // Validar que el proyecto tenga competidores asignados
        if (project.getCompetitors() == null || project.getCompetitors().isEmpty()) {
            throw new ValidationException("project", "El proyecto no tiene competidores asignados");
        }

        // Validar que no sea auto-voto (el votante no puede ser competidor del proyecto)
        boolean isSelfVote = project.getCompetitors().stream()
                .anyMatch(c -> c.getId().equals(voter.getId()));
        if (isSelfVote) {
            throw new ValidationException("competitor", "No puedes votar por tu propio proyecto");
        }

        // Cargar categoría si es proporcionada
        Category category = null;
        if (dto.getCategoryId() != null) {
            category = entityValidator.getCategoryOrThrow(dto.getCategoryId());
            validateCategoryVotingPeriod(category);
        }

        // Verificar si ya existe un voto para esta combinación
        java.util.Optional<Voting> existingOpt = votingRepository
                .findExistingVote(voter.getId(), project.getId(), criterion.getId(),
                        category != null ? category.getId() : null);

        if (existingOpt.isPresent()) {
            return handleExistingVote(existingOpt.get(), dto, category);
        }

        // Crear nuevo voto
        Voting voting = createNewVote(voter, project, criterion, dto, category);
        Voting saved = votingRepository.save(voting);
        notifyObservers(saved);

        return votingMapper.toDto(saved);
    }

    private Voting createNewVote(User voter, Project project, Criterion criterion,
                                 VotingDto dto, Category category) {
        Voting voting = new Voting(voter, project, criterion, dto.getScore());

        if (category != null) {
            voting.setCategory(category);
        }

        // Aplicar estrategia de ponderación si está disponible
        applyWeightingStrategy(voting, category);

        // Normalizar y establecer comentario
        voting.setComentario(normalizeComment(dto.getComentario()));

        // Validar según el tipo de votación
        evaluateVote(voting);

        return voting;
    }

    private VotingDto handleExistingVote(Voting existing, VotingDto dto, Category category) {
        int add = dto.getScore() != null ? dto.getScore() : 0;
        Category existingCat = existing.getCategory();

        if (existingCat != null && existingCat.getVotingType() == VotingType.JURY_EXPERT) {
            existing.setScore(add);
        } else {
            int current = existing.getScore() != null ? existing.getScore() : 0;
            existing.setScore(current + add);
        }

        if (category != null && existing.getCategory() == null) {
            existing.setCategory(category);
        }

        existing.setComentario(normalizeComment(dto.getComentario()));
        evaluateVote(existing);

        Voting saved = votingRepository.save(existing);
        notifyObservers(saved);
        return votingMapper.toDto(saved);
    }

    private void validateCategoryVotingPeriod(Category category) {
        if (category == null) {
            return;
        }

        long now = System.currentTimeMillis();

        if (category.getTimeInitial() != null && category.getTimeInitial().getTime() > now) {
            throw new ValidationException("timeInitial",
                    String.format("El periodo de votación no ha iniciado para la categoría '%s'", 
                            category.getName()));
        }

        if (category.getTimeFinal() != null && category.getTimeFinal().getTime() < now) {
            throw new ValidationException("timeFinal",
                    String.format("El periodo de votación ha finalizado para la categoría '%s'", 
                            category.getName()));
        }
    }

    private void applyWeightingStrategy(Voting voting, Category category) {
        try {
            com.votify.application.strategy.VoteWeightingStrategy strat = 
                    criterionService.getStrategyForCategory(category);
            if (strat != null) {
                double weighted = strat.applyWeight(voting, category);
                voting.setWeightedScore(weighted);
                voting.setWeightingStrategy(strat.key());
            }
        } catch (Exception ex) {
            // No bloquear la creación de votos si falla la selección de estrategia
        }
    }

    private boolean isPeriodActive(Category cat) {
        if (cat == null) return true;
        long now = System.currentTimeMillis();
        if (cat.getTimeInitial() != null && cat.getTimeInitial().getTime() > now) return false;
        if (cat.getTimeFinal() != null && cat.getTimeFinal().getTime() < now) return false;
        return true;
    }

    private void evaluateVote(Voting voting) {
        if (voting == null) {
            return;
        }

        Category category = voting.getCategory();
        if (category == null) {
            return;
        }

        // Usar el validador apropiado según el tipo de votación
        VotingValidator validator = votingValidatorFactory.getValidator(category);
        validator.validateVote(voting, category);
    }

    public VotingDto update(Long id, VotingDto dto) {
        Voting voting = votingRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new EntityNotFoundException("Voting", id));

        if (dto.getVoterId() != null && dto.getVoterId() > 0) {
            User voter = entityValidator.getUserOrThrow(dto.getVoterId());
            voting.setVoter(voter);
        }

        if (dto.getProjectId() != null && dto.getProjectId() > 0) {
            Project project = entityValidator.getProjectOrThrow(dto.getProjectId());
            voting.setProject(project);
        }

        if (dto.getCriterionId() != null && dto.getCriterionId() > 0) {
            Criterion criterion = entityValidator.getCriterionOrThrow(dto.getCriterionId());
            voting.setCriterion(criterion);
        }

        if (dto.getScore() != null) {
            voting.setScore(dto.getScore());
            applyWeightingStrategy(voting, voting.getCategory());
        }

        if (dto.getManuallyModified() != null) {
            voting.setManuallyModified(dto.getManuallyModified());
        }

        if (dto.getComentario() != null) {
            voting.setComentario(normalizeComment(dto.getComentario()));
        }

        return votingMapper.toDto(votingRepository.save(voting));
    }

    public void delete(Long id) {
        entityValidator.validateNonNull(id, "id");
        votingRepository.deleteById(id);
    }

    public List<VotingDto> findByProjectIds(List<Long> competitorIds) {
        return votingRepository.findByProjectIdIn(competitorIds)
                .stream()
                .map(votingMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<Long> getActiveVoterIds(Long categoryId) {
        return votingRepository.findDistinctVoterIdsByCategoryId(categoryId);
    }

    public List<VotingDto> findByVoterAndProject(Long voterId, Long competitorId) {
        return votingRepository.findByVoterIdAndProjectId(voterId, competitorId)
                .stream()
                .map(votingMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<VotingDto> findByVoterAndProjectAndCategory(Long voterId, Long competitorId, Long categoryId) {
        return votingRepository.findByVoterIdAndProjectIdAndCategoryId(voterId, competitorId, categoryId)
                .stream()
                .map(votingMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectRankingDto> getProjectsRanking(Long categoryId) {
        Map<Long, Double> competitorScores = new HashMap<>();
        for (Object[] row : votingRepository.findProjectScoresByCategoryId(categoryId)) {
            Long competitorId = (Long) row[0];
            Double score = ((Number) row[1]).doubleValue();
            competitorScores.put(competitorId, score);
        }

        List<ProjectRankingDto> ranking = new ArrayList<>();
        for (Project p : projectRepository.findByCategoryId(categoryId)) {
            double totalScore = p.getCompetitors().stream()
                    .mapToDouble(c -> competitorScores.getOrDefault(c.getId(), 0.0))
                    .sum();
            ranking.add(new ProjectRankingDto(p.getId(), p.getName(), (long) totalScore));
        }
        ranking.sort((a, b) -> Long.compare(b.getTotalScore(), a.getTotalScore()));
        return ranking;
    }

    private VotingDto toDto(Voting voting) {
        return votingMapper.toDto(voting);
    }

    private static final int COMENTARIO_MAX_LEN = 500;

    private String normalizeComment(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;
        if (trimmed.length() > COMENTARIO_MAX_LEN) {
            throw new RuntimeException("El comentario excede el máximo de "
                    + COMENTARIO_MAX_LEN + " caracteres.");
        }
        return trimmed;
    }
}
