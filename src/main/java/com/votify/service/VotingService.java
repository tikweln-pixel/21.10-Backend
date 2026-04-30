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
        if (dto.getVoterId() == null) throw new RuntimeException("El ID del votante no puede ser nulo");
        User voter = userRepository.findById(Objects.requireNonNull(dto.getVoterId()))
                .orElseThrow(() -> new RuntimeException("Voter not found with id: " + dto.getVoterId()));

        if (dto.getProjectId() == null) throw new RuntimeException("El ID del competidor no puede ser nulo");
        User competitor = userRepository.findById(Objects.requireNonNull(dto.getProjectId()))
                .orElseThrow(() -> new RuntimeException("Competitor not found with id: " + dto.getProjectId()));

        if (voter.getId().equals(competitor.getId())) {
            throw new RuntimeException("No puedes votarte a ti mismo.");
        }

        if (dto.getCriterionId() == null) throw new RuntimeException("El ID del criterio no puede ser nulo");
        Criterion criterion = criterionRepository.findById(Objects.requireNonNull(dto.getCriterionId()))
                .orElseThrow(() -> new RuntimeException("Criterion not found with id: " + dto.getCriterionId()));

        Long categoryId = dto.getCategoryId();
        Category category = null;
        if (categoryId != null) {
            category = categoryRepository.findById(Objects.requireNonNull(categoryId))
                    .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));
            if (!isPeriodActive(category)) {
                throw new RuntimeException("El periodo de votación no está activo para la categoría '" + category.getName() + "'.");
            }
        }

        java.util.Optional<Voting> existingOpt = votingRepository
                .findExistingVote(voter.getId(), competitor.getId(), criterion.getId(), categoryId);

        if (existingOpt.isPresent()) {
            Voting existing = existingOpt.get();
            int add = dto.getScore() != null ? dto.getScore() : 0;
            Category existingCat = existing.getCategory();
            if (existingCat != null && existingCat.getVotingType() == VotingType.JURY_EXPERT) {
                existing.setScore(add);
            } else {
                int current = existing.getScore() != null ? existing.getScore() : 0;
                existing.setScore(current + add);
            }
            if (categoryId != null && existing.getCategory() == null) {
                existing.setCategory(category);
            }
            existing.setComentario(normalizeComment(dto.getComentario()));
            evaluateVote(existing);
            return toDto(votingRepository.save(Objects.requireNonNull(existing)));
        }

        Voting voting = new Voting(voter, competitor, criterion, dto.getScore());
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
                    voting.getCompetitor().getId(),
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

    private void validatePopularVoteRestrictions(Long voterId, Long competitorId,
                                                  Category category, Integer score) {
        if (category.getMaxVotesPerVoter() != null) {
            long alreadyVotedCount = votingRepository
                    .countDistinctCompetitorsByVoterIdAndCategoryId(voterId, category.getId());

            boolean isNewCompetitor = true;
            List<Voting> existingVotes = votingRepository.findByVoterIdAndCategoryId(voterId, category.getId());
            for (Voting v : existingVotes) {
                if (v.getCompetitor().getId().equals(competitorId)) {
                    isNewCompetitor = false;
                    break;
                }
            }

            if (isNewCompetitor && alreadyVotedCount >= category.getMaxVotesPerVoter()) {
                throw new RuntimeException(
                        "El votante ya ha alcanzado el límite de " + category.getMaxVotesPerVoter()
                        + " competidores distintos permitidos en la categoría '" + category.getName() + "'.");
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
            Long vId = dto.getVoterId();
            User voter = userRepository.findById(Objects.requireNonNull(vId))
                    .orElseThrow(() -> new RuntimeException("Voter not found with id: " + vId));
            voting.setVoter(voter);
        }
        if (dto.getProjectId() != null && dto.getProjectId() > 0) {
            Long cId = dto.getProjectId();
            User competitor = userRepository.findById(Objects.requireNonNull(cId))
                    .orElseThrow(() -> new RuntimeException("Competitor not found with id: " + cId));
            voting.setCompetitor(competitor);
        }
        if (dto.getCriterionId() != null && dto.getCriterionId() > 0) {
            Long critId = dto.getCriterionId();
            Criterion criterion = criterionRepository.findById(Objects.requireNonNull(critId))
                    .orElseThrow(() -> new RuntimeException("Criterion not found with id: " + critId));
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

    public List<VotingDto> findByProjectIds(List<Long> competitorIds) {
        List<Voting> votings = votingRepository.findByCompetitorIdIn(competitorIds);
        List<VotingDto> result = new ArrayList<>();
        for (Voting voting : votings) {
            result.add(toDto(voting));
        }
        return result;
    }

    public List<Long> getActiveVoterIds(Long categoryId) {
        return votingRepository.findDistinctVoterIdsByCategoryId(categoryId);
    }

    public List<VotingDto> findByVoterAndProject(Long voterId, Long competitorId) {
        List<Voting> votings = votingRepository.findByVoterIdAndCompetitorId(voterId, competitorId);
        List<VotingDto> result = new ArrayList<>();
        for (Voting voting : votings) {
            result.add(toDto(voting));
        }
        return result;
    }

    public List<VotingDto> findByVoterAndProjectAndCategory(Long voterId, Long competitorId, Long categoryId) {
        List<Voting> votings = votingRepository.findByVoterIdAndCompetitorIdAndCategoryId(voterId, competitorId, categoryId);
        List<VotingDto> result = new ArrayList<>();
        for (Voting voting : votings) {
            result.add(toDto(voting));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<ProjectRankingDto> getProjectRanking(Long categoryId) {
        Map<Long, Double> competitorScores = new HashMap<>();
        for (Object[] row : votingRepository.findCompetitorScoresByCategoryId(categoryId)) {
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
        Long categoryId   = voting.getCategory()   != null ? voting.getCategory().getId()   : null;
        Long voterId      = voting.getVoter()       != null ? voting.getVoter().getId()      : null;
        Long competitorId = voting.getCompetitor()  != null ? voting.getCompetitor().getId() : null;
        Long criterionId  = voting.getCriterion()   != null ? voting.getCriterion().getId()  : null;
        VotingDto dto = new VotingDto(
                voting.getId(),
                voterId,
                competitorId,   // se expone como projectId en el DTO por compatibilidad con el frontend
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
