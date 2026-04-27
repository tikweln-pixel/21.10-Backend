package com.votify.controller;

import com.votify.dto.VotingDto;
import com.votify.service.VotingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/votings")
public class VotingController {

    private final VotingService votingService;

    public VotingController(VotingService votingService) {
        this.votingService = votingService;
    }

    @GetMapping
    public ResponseEntity<List<VotingDto>> getAll() {
        return ResponseEntity.ok(votingService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VotingDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(votingService.findById(id));
    }

    @PostMapping
    public ResponseEntity<VotingDto> create(@RequestBody VotingDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(votingService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VotingDto> update(@PathVariable Long id, @RequestBody VotingDto dto) {
        return ResponseEntity.ok(votingService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        votingService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-competitors")
    public ResponseEntity<List<VotingDto>> getByCompetitors(@RequestParam List<Long> ids) {
        return ResponseEntity.ok(votingService.findByCompetitorIds(ids));
    }

    @GetMapping("/by-voter-competitor")
    public ResponseEntity<List<VotingDto>> getByVoterAndCompetitor(
            @RequestParam Long voterId, @RequestParam Long competitorId) {
        return ResponseEntity.ok(votingService.findByVoterAndCompetitor(voterId, competitorId));
    }

    @GetMapping("/by-voter-competitor-category")
    public ResponseEntity<List<VotingDto>> getByVoterCompetitorCategory(
            @RequestParam Long voterId,
            @RequestParam Long competitorId,
            @RequestParam Long categoryId) {
        return ResponseEntity.ok(votingService.findByVoterAndCompetitorAndCategory(voterId, competitorId, categoryId));
    }
}
