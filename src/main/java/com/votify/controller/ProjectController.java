package com.votify.controller;

import com.votify.dto.CompetitorDto;
import com.votify.dto.ProjectDto;
import com.votify.dto.ProjectFinalScoreDto;
import com.votify.dto.CommentDto;
import com.votify.service.ProjectService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/projects")
    public ResponseEntity<List<ProjectDto>> getAll() {
        return ResponseEntity.ok(projectService.findAll());
    }

    @PostMapping("/events/{eventId}/projects")
    public ResponseEntity<ProjectDto> createForEvent(@PathVariable Long eventId,
                                                     @RequestBody ProjectDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.createForEvent(eventId, dto));
    }

    @GetMapping("/events/{eventId}/projects")
    public ResponseEntity<List<ProjectDto>> getByEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(projectService.findByEvent(eventId));
    }

    @PostMapping("/projects/{projectId}/competitors/{userId}")
    public ResponseEntity<ProjectDto> addCompetitor(@PathVariable Long projectId,
                                                    @PathVariable Long userId) {
        return ResponseEntity.ok(projectService.addCompetitor(projectId, userId));
    }

    @PostMapping("/projects/{projectId}/comments")
    public ResponseEntity<CommentDto> addComment(@PathVariable Long projectId,
                                                 @RequestBody CommentDto dto) {
        CommentDto saved = projectService.addComment(projectId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/projects/{projectId}/comments")
    public ResponseEntity<List<CommentDto>> getCommentsByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(projectService.getCommentsByProject(projectId));
    }

    @GetMapping("/projects/{projectId}/competitors")
    public ResponseEntity<List<CompetitorDto>> getCompetitors(@PathVariable Long projectId) {
        return ResponseEntity.ok(projectService.getCompetitors(projectId));
    }

    @GetMapping("/projects/{projectId}/score")
    public ResponseEntity<ProjectFinalScoreDto> getProjectScore(
            @PathVariable Long projectId,
            @RequestParam Long categoryId) {
        return ResponseEntity.ok(projectService.getProjectScore(projectId, categoryId));
    }
}

