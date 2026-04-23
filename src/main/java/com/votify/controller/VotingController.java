package com.votify.controller;

import com.votify.dto.ProjectRankingDto;
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

    @GetMapping("/by-projects")
    public ResponseEntity<List<VotingDto>> getByProjects(@RequestParam List<Long> ids) {
        return ResponseEntity.ok(votingService.findByProjectIds(ids));
    }

    @GetMapping("/by-voter-project")
    public ResponseEntity<List<VotingDto>> getByVoterAndProject(
            @RequestParam Long voterId, @RequestParam Long projectId) {
        return ResponseEntity.ok(votingService.findByVoterAndProject(voterId, projectId));
    }

    @GetMapping("/by-voter-project-category")
    public ResponseEntity<List<VotingDto>> getByVoterProjectCategory(
            @RequestParam Long voterId,
            @RequestParam Long projectId,
            @RequestParam Long categoryId) {
        return ResponseEntity.ok(votingService.findByVoterAndProjectAndCategory(voterId, projectId, categoryId));
    }

    @GetMapping("/ranking/by-category/{categoryId}")
    public ResponseEntity<List<ProjectRankingDto>> getRanking(@PathVariable Long categoryId) {
        return ResponseEntity.ok(votingService.getProjectRanking(categoryId));
    }
}
