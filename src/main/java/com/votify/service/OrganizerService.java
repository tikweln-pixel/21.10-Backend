package com.votify.service;

import com.votify.dto.OrganizerDto;
import com.votify.entity.Organizer;
import com.votify.entity.User;
import com.votify.persistence.OrganizerRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class OrganizerService {

    private final OrganizerRepository organizerRepository;

    public OrganizerService(OrganizerRepository organizerRepository) {
        this.organizerRepository = organizerRepository;
    }

    public List<OrganizerDto> findAll() {
        List<OrganizerDto> result = new ArrayList<>();
        for (User organizer : organizerRepository.findAll()) {
            result.add(toDto(organizer));
        }
        return result;
    }

    public OrganizerDto findById(Long id) {
        if (id == null) {
            throw new RuntimeException("El ID del organizador no puede ser nulo");
        }
        User organizer = organizerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Organizador no encontrado con id: " + id));
        return toDto(organizer);
    }

    public OrganizerDto create(OrganizerDto dto) {
        if (dto == null) {
            throw new RuntimeException("Los datos del organizador no pueden ser nulos");
        }
        User organizer = new Organizer(dto.getName(), dto.getEmail(), dto.getPassword());
        return toDto(organizerRepository.save(Objects.requireNonNull(organizer)));
    }

    public OrganizerDto update(Long id, OrganizerDto dto) {
        if (id == null) {
            throw new RuntimeException("El ID del organizador no puede ser nulo");
        }
        User organizer = organizerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Organizador no encontrado con id: " + id));
        organizer.setName(dto.getName());
        organizer.setEmail(dto.getEmail());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            organizer.setPassword(dto.getPassword());
        }
        return toDto(organizerRepository.save(organizer));
    }

    public void delete(Long id) {
        if (id == null) {
            throw new RuntimeException("El ID del organizador no puede ser nulo");
        }
        organizerRepository.deleteById(id);
    }

    private OrganizerDto toDto(User organizer) {
        return new OrganizerDto(organizer.getId(), organizer.getName(), organizer.getEmail());
    }
}
