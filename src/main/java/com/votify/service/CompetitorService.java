package com.votify.service;

import com.votify.dto.CompetitorDto;
import com.votify.entity.Competitor;
import com.votify.persistence.CompetitorRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class CompetitorService {

    private final CompetitorRepository competitorRepository;

    public CompetitorService(CompetitorRepository competitorRepository) {
        this.competitorRepository = competitorRepository;
    }

    public List<CompetitorDto> findAll() {
        List<Competitor> competitors = competitorRepository.findAll();
        List<CompetitorDto> result = new ArrayList<>();
        for (Competitor competitor : competitors) {
            result.add(toDto(competitor));
        }
        return result;
    }

    public CompetitorDto findById(Long id) {
        if (id == null) throw new RuntimeException("Competitor ID cannot be null");
        Competitor competitor = competitorRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("Competitor not found with id: " + id));
        return toDto(competitor);
    }

    public CompetitorDto create(CompetitorDto dto) {
        Competitor competitor = new Competitor(dto.getName(), dto.getEmail());
        return toDto(competitorRepository.save(Objects.requireNonNull(competitor)));
    }

    public CompetitorDto update(Long id, CompetitorDto dto) {
        if (id == null) throw new RuntimeException("Competitor ID cannot be null");
        Competitor competitor = competitorRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("Competitor not found with id: " + id));
        competitor.setName(dto.getName());
        competitor.setEmail(dto.getEmail());
        return toDto(competitorRepository.save(Objects.requireNonNull(competitor)));
    }

    public void delete(Long id) {
        if (id == null) throw new RuntimeException("Competitor ID cannot be null");
        competitorRepository.deleteById(Objects.requireNonNull(id));
    }

    private CompetitorDto toDto(Competitor competitor) {
        return new CompetitorDto(competitor.getId(), competitor.getName(), competitor.getEmail());
    }
}
