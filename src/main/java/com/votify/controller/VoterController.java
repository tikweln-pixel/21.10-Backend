package com.votify.controller;

import com.votify.dto.VoterDto;
import com.votify.service.VoterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/voters")
public class VoterController {

    private final VoterService voterService;

    public VoterController(VoterService voterService) {
        this.voterService = voterService;
    }

    @GetMapping
    public ResponseEntity<List<VoterDto>> getAll() {
        return ResponseEntity.ok(voterService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VoterDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(voterService.findById(id));
    }

    @PostMapping
    public ResponseEntity<VoterDto> create(@RequestBody VoterDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(voterService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VoterDto> update(@PathVariable Long id, @RequestBody VoterDto dto) {
        return ResponseEntity.ok(voterService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        voterService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
