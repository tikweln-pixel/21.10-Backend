package com.votify.controller;

import com.votify.dto.ParticipantDto;
import com.votify.service.ParticipantService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/participants")
public class ParticipantController {

    private final ParticipantService participantService;

    public ParticipantController(ParticipantService participantService) {
        this.participantService = participantService;
    }

    @GetMapping
    public ResponseEntity<List<ParticipantDto>> getAll() {
        return ResponseEntity.ok(participantService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ParticipantDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(participantService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ParticipantDto> create(@RequestBody ParticipantDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(participantService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ParticipantDto> update(@PathVariable Long id, @RequestBody ParticipantDto dto) {
        return ResponseEntity.ok(participantService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        participantService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
