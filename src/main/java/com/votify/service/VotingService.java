package com.votify.service;

import com.votify.dto.VotingDto;
import com.votify.entity.*;
import com.votify.persistence.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VotingService {

    private final VotingRepository votingRepository;
    private final VoterRepository voterRepository;
    private final CompetitorRepository competitorRepository;
    private final CriterionRepository criterionRepository;
    private final CategoryRepository categoryRepository;

    public VotingService(VotingRepository votingRepository,
                         VoterRepository voterRepository,
                         CompetitorRepository competitorRepository,
                         CriterionRepository criterionRepository,
                         CategoryRepository categoryRepository) {
        this.votingRepository = votingRepository;
        this.voterRepository = voterRepository;
        this.competitorRepository = competitorRepository;
        this.criterionRepository = criterionRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<VotingDto> findAll() {
        return votingRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public VotingDto findById(Long id) {
        Voting voting = votingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Voting not found with id: " + id));
        return toDto(voting);
    }

    public VotingDto create(VotingDto dto) {
        Voter voter = voterRepository.findById(dto.getVoterId())
                .orElseThrow(() -> new RuntimeException("Voter not found with id: " + dto.getVoterId()));
        Competitor competitor = competitorRepository.findById(dto.getCompetitorId())
                .orElseThrow(() -> new RuntimeException("Competitor not found with id: " + dto.getCompetitorId()));
        Criterion criterion = criterionRepository.findById(dto.getCriterionId())
                .orElseThrow(() -> new RuntimeException("Criterion not found with id: " + dto.getCriterionId()));

        Voting voting = new Voting(voter, competitor, criterion, dto.getScore());

        // Req. 19/23 – Si se indica categoría, enlazar y aplicar restricciones POPULAR_VOTE
        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found with id: " + dto.getCategoryId()));
            voting.setCategory(category);

            if (category.getVotingType() == VotingType.POPULAR_VOTE) {
                validatePopularVoteRestrictions(voter.getId(), competitor.getId(), category, dto.getScore());
            }
        }

        return toDto(votingRepository.save(voting));
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
        Voting voting = votingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Voting not found with id: " + id));

        Voter voter = voterRepository.findById(dto.getVoterId())
                .orElseThrow(() -> new RuntimeException("Voter not found with id: " + dto.getVoterId()));
        Competitor competitor = competitorRepository.findById(dto.getCompetitorId())
                .orElseThrow(() -> new RuntimeException("Competitor not found with id: " + dto.getCompetitorId()));
        Criterion criterion = criterionRepository.findById(dto.getCriterionId())
                .orElseThrow(() -> new RuntimeException("Criterion not found with id: " + dto.getCriterionId()));

        voting.setVoter(voter);
        voting.setCompetitor(competitor);
        voting.setCriterion(criterion);
        voting.setScore(dto.getScore());
        return toDto(votingRepository.save(voting));
    }

    public void delete(Long id) {
        votingRepository.deleteById(id);
    }

    private VotingDto toDto(Voting voting) {
        Long categoryId = voting.getCategory() != null ? voting.getCategory().getId() : null;
        return new VotingDto(
                voting.getId(),
                voting.getVoter().getId(),
                voting.getCompetitor().getId(),
                voting.getCriterion().getId(),
                voting.getScore(),
                categoryId
        );
    }
}
