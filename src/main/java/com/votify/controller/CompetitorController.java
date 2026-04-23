package com.votify.controller;

import com.votify.dto.CompetitorCommentDto;
import com.votify.dto.CompetitorDto;
import com.votify.entity.Comment;
import com.votify.entity.Project;
import com.votify.entity.User;
import com.votify.persistence.CommentRepository;
import com.votify.persistence.ProjectRepository;
import com.votify.persistence.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/competitors")
public class CompetitorController {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final CommentRepository commentRepository;

    public CompetitorController(UserRepository userRepository,
                                ProjectRepository projectRepository,
                                CommentRepository commentRepository) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.commentRepository = commentRepository;
    }

    @GetMapping
    public ResponseEntity<List<CompetitorDto>> getAll() {
        List<CompetitorDto> result = new ArrayList<>();
        for (User user : userRepository.findAll()) {
            result.add(new CompetitorDto(user.getId(), user.getName(), user.getEmail()));
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompetitorDto> getById(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return ResponseEntity.ok(new CompetitorDto(user.getId(), user.getName(), user.getEmail()));
    }

    @GetMapping("/{competitorId}/comments")
    public ResponseEntity<List<CompetitorCommentDto>> getCommentsByCompetitor(@PathVariable Long competitorId) {
        List<Project> projects = new ArrayList<>();
        for (Project p : projectRepository.findAll()) {
            for (User c : p.getCompetitors()) {
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
            Long voterId = c.getVoter() != null ? c.getVoter().getId() : null;
            comments.add(new CompetitorCommentDto(
                    c.getId(), c.getText(), voterId,
                    c.getProject().getId(), c.getProject().getName()));
        }

        return ResponseEntity.ok(comments);
    }
}
