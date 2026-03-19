package com.votify.service;

import com.votify.dto.ParticipantDto;
import com.votify.entity.Participant;
import com.votify.persistence.ParticipantRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ParticipantService {

    private final ParticipantRepository participantRepository;

    public ParticipantService(ParticipantRepository participantRepository) {
        this.participantRepository = participantRepository;
    }

    public List<ParticipantDto> findAll() {
        return participantRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public ParticipantDto findById(Long id) {
        Participant participant = participantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Participant not found with id: " + id));
        return toDto(participant);
    }

    public ParticipantDto create(ParticipantDto dto) {
        Participant participant = new Participant(dto.getName(), dto.getEmail());
        return toDto(participantRepository.save(participant));
    }

    public ParticipantDto update(Long id, ParticipantDto dto) {
        Participant participant = participantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Participant not found with id: " + id));
        participant.setName(dto.getName());
        participant.setEmail(dto.getEmail());
        return toDto(participantRepository.save(participant));
    }

    public void delete(Long id) {
        participantRepository.deleteById(id);
    }

    private ParticipantDto toDto(Participant participant) {
        return new ParticipantDto(participant.getId(), participant.getName(), participant.getEmail());
    }
}
