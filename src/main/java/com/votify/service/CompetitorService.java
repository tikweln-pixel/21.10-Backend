package com.votify.service;

import com.votify.dto.CompetitorDto;
import com.votify.entity.Competitor;
import com.votify.persistence.CompetitorRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CompetitorService {

    private final CompetitorRepository competitorRepository;

    public CompetitorService(CompetitorRepository competitorRepository) {
        this.competitorRepository = competitorRepository;
    }

    public List<CompetitorDto> findAll() {
        return competitorRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public CompetitorDto findById(Long id) {
        Competitor competitor = competitorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Competitor not found with id: " + id));
        return toDto(competitor);
    }

    public CompetitorDto create(CompetitorDto dto) {
        Competitor competitor = new Competitor(dto.getName(), dto.getEmail());
        return toDto(competitorRepository.save(competitor));
    }

    public CompetitorDto update(Long id, CompetitorDto dto) {
        Competitor competitor = competitorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Competitor not found with id: " + id));
        competitor.setName(dto.getName());
        competitor.setEmail(dto.getEmail());
        return toDto(competitorRepository.save(competitor));
    }

    public void delete(Long id) {
        competitorRepository.deleteById(id);
    }

    private CompetitorDto toDto(Competitor competitor) {
        return new CompetitorDto(competitor.getId(), competitor.getName(), competitor.getEmail());
    }
}
