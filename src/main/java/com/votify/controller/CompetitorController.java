package com.votify.controller;

import com.votify.dto.CompetitorCommentDto;
import com.votify.dto.CompetitorDto;
import com.votify.entity.Comment;
import com.votify.entity.Competitor;
import com.votify.entity.Project;
import com.votify.persistence.CommentRepository;
import com.votify.persistence.ProjectRepository;
import com.votify.service.CompetitorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/competitors")
public class CompetitorController {

    private final CompetitorService competitorService;
    private final ProjectRepository projectRepository;
    private final CommentRepository commentRepository;

    public CompetitorController(CompetitorService competitorService,
                                ProjectRepository projectRepository,
                                CommentRepository commentRepository) {
        this.competitorService = competitorService;
        this.projectRepository = projectRepository;
        this.commentRepository = commentRepository;
    }

    @GetMapping
    public ResponseEntity<List<CompetitorDto>> getAll() {
        return ResponseEntity.ok(competitorService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompetitorDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(competitorService.findById(id));
    }

    @PostMapping
    public ResponseEntity<CompetitorDto> create(@RequestBody CompetitorDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(competitorService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompetitorDto> update(@PathVariable Long id, @RequestBody CompetitorDto dto) {
        return ResponseEntity.ok(competitorService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        competitorService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{competitorId}/comments")
    public ResponseEntity<List<CompetitorCommentDto>> getCommentsByCompetitor(@PathVariable Long competitorId) {
        List<Project> projects = new ArrayList<>();
        for (Project p : projectRepository.findAll()) {
            for (Competitor c : p.getCompetitors()) {
                if (c.getId().equals(competitorId)) {
                    projects.add(p);
                    break;
                }
            }
        }

        List<Long> projectIds = new ArrayList<>();
        for (Project p : projects) {
            projectIds.add(p.getId());
        }

        if (projectIds.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<CompetitorCommentDto> comments = new ArrayList<>();
        for (Comment c : commentRepository.findByProjectIdIn(projectIds)) {
            comments.add(new CompetitorCommentDto(
                    c.getId(), c.getText(), c.getVoter().getId(),
                    c.getProject().getId(), c.getProject().getName()));
        }

        return ResponseEntity.ok(comments);
    }
}
