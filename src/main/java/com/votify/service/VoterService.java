package com.votify.service;

import com.votify.dto.VoterDto;
import com.votify.entity.Voter;
import com.votify.persistence.VoterRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VoterService {

    private final VoterRepository voterRepository;

    public VoterService(VoterRepository voterRepository) {
        this.voterRepository = voterRepository;
    }

    public List<VoterDto> findAll() {
        return voterRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public VoterDto findById(Long id) {
        Voter voter = voterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Voter not found with id: " + id));
        return toDto(voter);
    }

    public VoterDto create(VoterDto dto) {
        Voter voter = new Voter(dto.getName(), dto.getEmail());
        return toDto(voterRepository.save(voter));
    }

    public VoterDto update(Long id, VoterDto dto) {
        Voter voter = voterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Voter not found with id: " + id));
        voter.setName(dto.getName());
        voter.setEmail(dto.getEmail());
        return toDto(voterRepository.save(voter));
    }

    public void delete(Long id) {
        voterRepository.deleteById(id);
    }

    private VoterDto toDto(Voter voter) {
        return new VoterDto(voter.getId(), voter.getName(), voter.getEmail());
    }
}
