package com.votify.service;

import com.votify.dto.VotingDto;
import com.votify.entity.*;
import com.votify.persistence.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Service
public class VotingService {

    private final VotingRepository votingRepository;
    private final VoterRepository voterRepository;
    private final CompetitorRepository competitorRepository;
    private final CriterionRepository criterionRepository;
    private final CategoryRepository categoryRepository;
    private final com.votify.persistence.CategoryCriterionPointsRepository criterionPointsRepository;

    public VotingService(VotingRepository votingRepository,
                         VoterRepository voterRepository,
                         CompetitorRepository competitorRepository,
                         CriterionRepository criterionRepository,
                         CategoryRepository categoryRepository,
                         com.votify.persistence.CategoryCriterionPointsRepository criterionPointsRepository) {
        this.votingRepository = votingRepository;
        this.voterRepository = voterRepository;
        this.competitorRepository = competitorRepository;
        this.criterionRepository = criterionRepository;
        this.categoryRepository = categoryRepository;
        this.criterionPointsRepository = criterionPointsRepository;
    }

    public List<VotingDto> findAll() {
        return votingRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public VotingDto findById(Long id) {
        if (id == null) throw new RuntimeException("Voting ID cannot be null");
        Voting voting = votingRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("Voting not found with id: " + id));
        return toDto(voting);
    }

    public VotingDto create(VotingDto dto) {
        if (dto.getVoterId() == null) throw new RuntimeException("Voter ID cannot be null");
        Voter voter = voterRepository.findById(Objects.requireNonNull(dto.getVoterId()))
                .orElseThrow(() -> new RuntimeException("Voter not found with id: " + dto.getVoterId()));
        if (dto.getCompetitorId() == null) throw new RuntimeException("Competitor ID cannot be null");
        Competitor competitor = competitorRepository.findById(Objects.requireNonNull(dto.getCompetitorId()))
                .orElseThrow(() -> new RuntimeException("Competitor not found with id: " + dto.getCompetitorId()));
        if (dto.getCriterionId() == null) throw new RuntimeException("Criterion ID cannot be null");
        Criterion criterion = criterionRepository.findById(Objects.requireNonNull(dto.getCriterionId()))
                .orElseThrow(() -> new RuntimeException("Criterion not found with id: " + dto.getCriterionId()));
        // Buscar si ya existe un voto del mismo votante para el mismo competidor/criterio/categoría
        Long categoryId = dto.getCategoryId();
        java.util.Optional<Voting> existingOpt = votingRepository
                .findExistingVote(voter.getId(), competitor.getId(), criterion.getId(), categoryId);

        if (existingOpt.isPresent()) {
            Voting existing = existingOpt.get();
            int add = dto.getScore() != null ? dto.getScore() : 0;
            // JURY_EXPERT: el jurado sobreescribe su puntuación anterior (no acumula)
            // POPULAR_VOTE (y sin categoría): se acumula como votos populares
            Category existingCat = existing.getCategory();
            if (existingCat != null && existingCat.getVotingType() == VotingType.JURY_EXPERT) {
                existing.setScore(add);
            } else {
                int current = existing.getScore() != null ? existing.getScore() : 0;
                existing.setScore(current + add);
            }

            // Aseguramos que la categoría esté enlazada para las validaciones
            if (categoryId != null && existing.getCategory() == null) {
                Category category = categoryRepository.findById(Objects.requireNonNull(categoryId))
                        .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));
                existing.setCategory(category);
            }

            // Evaluar reglas según tipo de votación (se pasa el voto actualizado)
            evaluateVote(existing);
            return toDto(votingRepository.save(Objects.requireNonNull(existing)));
        }

        // Si no existe, creamos un nuevo voto
        Voting voting = new Voting(voter, competitor, criterion, dto.getScore());
        if (categoryId != null) {
            Category category = categoryRepository.findById(Objects.requireNonNull(categoryId))
                    .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));
            voting.setCategory(category);
        }

        // Evaluar el voto según el tipo de votación de la categoría (si aplica)
        evaluateVote(voting);

        return toDto(votingRepository.save(Objects.requireNonNull(voting)));
    }

    /**
     * Evalúa un voto aplicando las reglas correspondientes según el VotingType
     * asociado a la categoría (si la hay).
     *
     * Actualmente implementa las validaciones para POPULAR_VOTE. Para otros tipos
     * de votación (p.ej. JURY_EXPERT) no se aplica validación adicional aquí.
     */
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
            // Para JURY_EXPERT validamos que el score no exceda los weightPercent
            // configurados para ese criterio en la categoría.
            if (criterionPointsRepository == null) {
                throw new RuntimeException("CategoryCriterionPointsRepository no disponible para validar JURY_EXPERT");
            }
            Long categoryId = category.getId();
            Long criterionId = voting.getCriterion().getId();
            com.votify.entity.CategoryCriterionPoints points = criterionPointsRepository
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

    /**
     * Req. 19 – Restricción POPULAR_VOTE:
     * Valida que el votante no supere el límite de competidores distintos (maxVotesPerVoter)
     * ni el total de puntos configurado (totalPoints) para la categoría.
     *
     * Regla de negocio: con 5 proyectos se puede votar hasta 3 en una votación popular.
     */
    private void validatePopularVoteRestrictions(Long voterId, Long competitorId,
                                                  Category category, Integer score) {
        // Validación 1: límite de competidores distintos
        if (category.getMaxVotesPerVoter() != null) {
            long alreadyVotedCount = votingRepository
                    .countDistinctCompetitorsByVoterIdAndCategoryId(voterId, category.getId());

            // Si el competidor ya ha recibido voto de este votante, no cuenta como nuevo
            boolean isNewCompetitor = votingRepository
                    .findByVoterIdAndCategoryId(voterId, category.getId())
                    .stream()
                    .noneMatch(v -> v.getCompetitor().getId().equals(competitorId));

            if (isNewCompetitor && alreadyVotedCount >= category.getMaxVotesPerVoter()) {
                throw new RuntimeException(
                        "El votante ya ha alcanzado el límite de " + category.getMaxVotesPerVoter()
                        + " competidores distintos permitidos en la categoría '" + category.getName() + "'.");
            }
        }

        // Validación 2: total de puntos no supera el configurado
        if (category.getTotalPoints() != null && score != null) {
            int alreadyUsedPoints = votingRepository
                    .sumScoreByVoterIdAndCategoryId(voterId, category.getId());
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
            Voter voter = voterRepository.findById(Objects.requireNonNull(vId))
                    .orElseThrow(() -> new RuntimeException("Voter not found with id: " + vId));
            voting.setVoter(voter);
        }
        if (dto.getCompetitorId() != null && dto.getCompetitorId() > 0) {
            Long cId = dto.getCompetitorId();
            Competitor competitor = competitorRepository.findById(Objects.requireNonNull(cId))
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
        return toDto(votingRepository.save(Objects.requireNonNull(voting)));
    }

    public void delete(Long id) {
        if (id == null) throw new RuntimeException("Voting ID cannot be null");
        votingRepository.deleteById(Objects.requireNonNull(id));
    }

    public List<VotingDto> findByCompetitorIds(List<Long> competitorIds) {
        return votingRepository.findByCompetitorIdIn(competitorIds).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<Long> getActiveVoterIds(Long categoryId) {
        return votingRepository.findDistinctVoterIdsByCategoryId(categoryId);
    }

    public List<VotingDto> findByVoterAndCompetitor(Long voterId, Long competitorId) {
        return votingRepository.findByVoterIdAndCompetitorId(voterId, competitorId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<VotingDto> findByVoterAndCompetitorAndCategory(Long voterId, Long competitorId, Long categoryId) {
        return votingRepository.findByVoterIdAndCompetitorIdAndCategoryId(voterId, competitorId, categoryId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private VotingDto toDto(Voting voting) {
        Long categoryId = voting.getCategory() != null ? voting.getCategory().getId() : null;
        return new VotingDto(
                voting.getId(),
                voting.getVoter().getId(),
                voting.getCompetitor().getId(),
                voting.getCriterion().getId(),
                voting.getScore(),
                categoryId,
                voting.getManuallyModified()
        );
    }
}
