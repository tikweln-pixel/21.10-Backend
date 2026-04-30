package com.votify.service;

import com.votify.dto.ProjectRankingDto;
import com.votify.dto.VotingDto;
import com.votify.entity.*;
import com.votify.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@Service
public class VotingService {

    private final VotingRepository votingRepository;
    private final UserRepository userRepository;
    private final CriterionRepository criterionRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryCriterionPointsRepository criterionPointsRepository;
    private final ProjectRepository projectRepository;
    private final CriterionService criterionService;

    public VotingService(VotingRepository votingRepository,
                         UserRepository userRepository,
                         CriterionRepository criterionRepository,
                         CategoryRepository categoryRepository,
                         CategoryCriterionPointsRepository criterionPointsRepository,
                         ProjectRepository projectRepository,
                         CriterionService criterionService) {
        this.votingRepository = votingRepository;
        this.userRepository = userRepository;
        this.criterionRepository = criterionRepository;
        this.categoryRepository = categoryRepository;
        this.criterionPointsRepository = criterionPointsRepository;
        this.projectRepository = projectRepository;
        this.criterionService = criterionService;
    }

    public List<VotingDto> findAll() {
        List<Voting> votings = votingRepository.findAll();
        List<VotingDto> result = new ArrayList<>();
        for (Voting voting : votings) {
            result.add(toDto(voting));
        }
        return result;
    }

    public VotingDto findById(Long id) {
        if (id == null) throw new RuntimeException("El ID del voto no puede ser nulo");
        Voting voting = votingRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("Voting not found with id: " + id));
        return toDto(voting);
    }

    public VotingDto create(VotingDto dto) {
        // ── Validar votante ───────────────────────────────────────────────
        if (dto.getVoterId() == null) throw new RuntimeException("El ID del votante no puede ser nulo");
        User voter = userRepository.findById(dto.getVoterId())
                .orElseThrow(() -> new RuntimeException("Voter not found with id: " + dto.getVoterId()));

        // ── Validar proyecto ──────────────────────────────────────────────
        if (dto.getProjectId() == null) throw new RuntimeException("El ID del proyecto no puede ser nulo");
        Project project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + dto.getProjectId()));

        // ── Validar que el proyecto tiene competidores asignados ──────────
        if (project.getCompetitors() == null || project.getCompetitors().isEmpty()) {
            throw new RuntimeException("El proyecto '" + project.getName() + "' no tiene competidores asignados.");
        }

        // ── Validar auto-voto: el votante no puede ser competidor del proyecto ──
        boolean isSelfVote = project.getCompetitors().stream()
                .anyMatch(c -> c.getId().equals(voter.getId()));
        if (isSelfVote) {
            throw new RuntimeException("No puedes votar tu propio proyecto.");
        }

        // ── Validar criterio ──────────────────────────────────────────────
        if (dto.getCriterionId() == null) throw new RuntimeException("El ID del criterio no puede ser nulo");
        Criterion criterion = criterionRepository.findById(dto.getCriterionId())
                .orElseThrow(() -> new RuntimeException("Criterion not found with id: " + dto.getCriterionId()));

        // ── Validar categoría y periodo ───────────────────────────────────
        Long categoryId = dto.getCategoryId();
        Category category = null;
        if (categoryId != null) {
            category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));
            if (!isPeriodActive(category)) {
                throw new RuntimeException("El periodo de votación no está activo para la categoría '" + category.getName() + "'.");
            }
        }

        // ── PA-1314 Guard Clause — rechazar voto duplicado ───────────────
        // Si ya existe voto para (voter, project, criterion, category) se lanza
        // excepción explícita. La actualización intencionada se hace exclusivamente
        // via PUT /api/votings/{id} (VotingService.update()), que es el usado por
        // IntervencionManual.tsx con manuallyModified=true.
        if (votingRepository.findExistingVote(
                voter.getId(), project.getId(), criterion.getId(), categoryId).isPresent()) {
            throw new RuntimeException("Ya has votado este proyecto.");
        }

        // ── Crear nuevo voto ──────────────────────────────────────────────
        Voting voting = new Voting(voter, project, criterion, dto.getScore());
        if (category != null) {
            voting.setCategory(category);
        }

        // Aplicar estrategia de ponderación si está disponible
        try {
            com.votify.application.strategy.VoteWeightingStrategy strat = criterionService.getStrategyForCategory(category);
            if (strat != null) {
                double weighted = strat.applyWeight(voting, category);
                voting.setWeightedScore(weighted);
                voting.setWeightingStrategy(strat.key());
            }
        } catch (Exception ex) {
            // no bloquear la creación de votos si falla la selección de estrategia
        }

        voting.setComentario(normalizeComment(dto.getComentario()));
        evaluateVote(voting);
        return toDto(votingRepository.save(Objects.requireNonNull(voting)));
    }

    private boolean isPeriodActive(Category cat) {
        if (cat == null) return true;
        long now = System.currentTimeMillis();
        if (cat.getTimeInitial() != null && cat.getTimeInitial().getTime() > now) return false;
        if (cat.getTimeFinal() != null && cat.getTimeFinal().getTime() < now) return false;
        return true;
    }

    private void evaluateVote(Voting voting) {
        if (voting == null) return;
        Category category = voting.getCategory();
        if (category == null) return;

        if (category.getVotingType() == VotingType.POPULAR_VOTE) {
            validatePopularVoteRestrictions(
                    voting.getVoter().getId(),
                    voting.getProject().getId(),
                    category,
                    voting.getScore()
            );
        } else if (category.getVotingType() == VotingType.JURY_EXPERT) {
            if (criterionPointsRepository == null) {
                throw new RuntimeException("CategoryCriterionPointsRepository no disponible para validar el tipo JURY_EXPERT");
            }
            Long categoryId = category.getId();
            Long criterionId = voting.getCriterion().getId();
            CategoryCriterionPoints points = criterionPointsRepository
                    .findByCategoryIdAndCriterionId(categoryId, criterionId)
                    .orElseThrow(() -> new RuntimeException(
                            "No hay configuración de puntos para el criterio '" + voting.getCriterion().getName()
                            + "' en la categoría '" + category.getName() + "'."));

            Integer weightPercent = points.getWeightPercent();
            Integer score = voting.getScore();
            if (score != null && weightPercent != null && score > weightPercent) {
                throw new RuntimeException(
                        "El score (" + score + ") excede los puntos máximos (" + weightPercent + ") para el criterio '"
                        + voting.getCriterion().getName() + "' en la categoría '" + category.getName() + "'.");
            }
        }
    }

    private void validatePopularVoteRestrictions(Long voterId, Long projectId,
                                                  Category category, Integer score) {
        if (category.getMaxVotesPerVoter() != null) {
            long alreadyVotedCount = votingRepository
                    .countDistinctProjectsByVoterIdAndCategoryId(voterId, category.getId());

            boolean isNewProject = true;
            List<Voting> existingVotes = votingRepository.findByVoterIdAndCategoryId(voterId, category.getId());
            for (Voting v : existingVotes) {
                if (v.getProject().getId().equals(projectId)) {
                    isNewProject = false;
                    break;
                }
            }

            if (isNewProject && alreadyVotedCount >= category.getMaxVotesPerVoter()) {
                throw new RuntimeException(
                        "El votante ya ha alcanzado el límite de " + category.getMaxVotesPerVoter()
                        + " proyectos distintos permitidos en la categoría '" + category.getName() + "'.");
            }
        }

        if (category.getTotalPoints() != null && score != null) {
            int alreadyUsedPoints = votingRepository.sumScoreByVoterIdAndCategoryId(voterId, category.getId());
            if (alreadyUsedPoints + score > category.getTotalPoints()) {
                throw new RuntimeException(
                        "El votante superaría el total de puntos permitidos (" + category.getTotalPoints()
                        + ") en la categoría '" + category.getName() + "'. "
                        + "Puntos ya usados: " + alreadyUsedPoints + ", puntos solicitados: " + score + ".");
            }
        }
    }

    public VotingDto update(Long id, VotingDto dto) {
        Voting voting = votingRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("Voting not found with id: " + id));

        if (dto.getVoterId() != null && dto.getVoterId() > 0) {
            User voter = userRepository.findById(dto.getVoterId())
                    .orElseThrow(() -> new RuntimeException("Voter not found with id: " + dto.getVoterId()));
            voting.setVoter(voter);
        }
        if (dto.getProjectId() != null && dto.getProjectId() > 0) {
            Project project = projectRepository.findById(dto.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Project not found with id: " + dto.getProjectId()));
            voting.setProject(project);
        }
        if (dto.getCriterionId() != null && dto.getCriterionId() > 0) {
            Criterion criterion = criterionRepository.findById(dto.getCriterionId())
                    .orElseThrow(() -> new RuntimeException("Criterion not found with id: " + dto.getCriterionId()));
            voting.setCriterion(criterion);
        }
        if (dto.getScore() != null) {
            voting.setScore(dto.getScore());
        }
        if (dto.getManuallyModified() != null) {
            voting.setManuallyModified(dto.getManuallyModified());
        }
        if (dto.getComentario() != null) {
            voting.setComentario(normalizeComment(dto.getComentario()));
        }
        return toDto(votingRepository.save(Objects.requireNonNull(voting)));
    }

    public void delete(Long id) {
        if (id == null) throw new RuntimeException("El ID del voto no puede ser nulo");
        votingRepository.deleteById(Objects.requireNonNull(id));
    }

    public List<VotingDto> findByProjectIds(List<Long> projectIds) {
        List<Voting> votings = votingRepository.findByProjectIdIn(projectIds);
        List<VotingDto> result = new ArrayList<>();
        for (Voting voting : votings) {
            result.add(toDto(voting));
        }
        return result;
    }

    public List<Long> getActiveVoterIds(Long categoryId) {
        return votingRepository.findDistinctVoterIdsByCategoryId(categoryId);
    }

    public List<VotingDto> findByVoterAndProject(Long voterId, Long projectId) {
        List<Voting> votings = votingRepository.findByVoterIdAndProjectId(voterId, projectId);
        List<VotingDto> result = new ArrayList<>();
        for (Voting voting : votings) {
            result.add(toDto(voting));
        }
        return result;
    }

    public List<VotingDto> findByVoterAndProjectAndCategory(Long voterId, Long projectId, Long categoryId) {
        List<Voting> votings = votingRepository.findByVoterIdAndProjectIdAndCategoryId(voterId, projectId, categoryId);
        List<VotingDto> result = new ArrayList<>();
        for (Voting voting : votings) {
            result.add(toDto(voting));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<ProjectRankingDto> getProjectRanking(Long categoryId) {
        Map<Long, Double> projectScores = new HashMap<>();
        for (Object[] row : votingRepository.findProjectScoresByCategoryId(categoryId)) {
            Long projectId = (Long) row[0];
            Double score = ((Number) row[1]).doubleValue();
            projectScores.put(projectId, score);
        }

        List<ProjectRankingDto> ranking = new ArrayList<>();
        for (Project p : projectRepository.findByCategoryId(categoryId)) {
            double totalScore = projectScores.getOrDefault(p.getId(), 0.0);
            ranking.add(new ProjectRankingDto(p.getId(), p.getName(), (long) totalScore));
        }
        ranking.sort((a, b) -> Long.compare(b.getTotalScore(), a.getTotalScore()));
        return ranking;
    }

    private VotingDto toDto(Voting voting) {
        Long categoryId  = voting.getCategory()  != null ? voting.getCategory().getId()  : null;
        Long voterId     = voting.getVoter()      != null ? voting.getVoter().getId()     : null;
        Long projectId   = voting.getProject()    != null ? voting.getProject().getId()   : null;
        Long criterionId = voting.getCriterion()  != null ? voting.getCriterion().getId() : null;
        VotingDto dto = new VotingDto(
                voting.getId(),
                voterId,
                projectId,
                criterionId,
                voting.getScore(),
                categoryId,
                voting.getManuallyModified()
        );
        dto.setComentario(voting.getComentario());
        dto.setWeightedScore(voting.getWeightedScore());
        dto.setWeightingStrategy(voting.getWeightingStrategy());
        return dto;
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
