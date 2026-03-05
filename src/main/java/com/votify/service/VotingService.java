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

    public VotingService(VotingRepository votingRepository,
                         VoterRepository voterRepository,
                         CompetitorRepository competitorRepository,
                         CriterionRepository criterionRepository) {
        this.votingRepository = votingRepository;
        this.voterRepository = voterRepository;
        this.competitorRepository = competitorRepository;
        this.criterionRepository = criterionRepository;
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
        return toDto(votingRepository.save(voting));
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
        return new VotingDto(
                voting.getId(),
                voting.getVoter().getId(),
                voting.getCompetitor().getId(),
                voting.getCriterion().getId(),
                voting.getScore()
        );
    }
}
