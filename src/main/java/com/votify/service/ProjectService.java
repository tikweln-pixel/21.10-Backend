package com.votify.service;

import com.votify.dto.CompetitorDto;
import com.votify.dto.CommentDto;
import com.votify.dto.ProjectDto;
import com.votify.dto.ProjectFinalScoreDto;
import com.votify.entity.*;
import com.votify.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final CategoryCriterionPointsRepository criterionPointsRepository;
    private final VotingRepository votingRepository;
    private final EventParticipationService eventParticipationService;

    public ProjectService(ProjectRepository projectRepository,
                          EventRepository eventRepository,
                          CategoryRepository categoryRepository,
                          CommentRepository commentRepository,
                          UserRepository userRepository,
                          CategoryCriterionPointsRepository criterionPointsRepository,
                          VotingRepository votingRepository,
                          EventParticipationService eventParticipationService) {
        this.projectRepository = projectRepository;
        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.criterionPointsRepository = criterionPointsRepository;
        this.votingRepository = votingRepository;
        this.eventParticipationService = eventParticipationService;
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> findAll() {
        List<Project> projects = projectRepository.findAll();
        List<ProjectDto> result = new ArrayList<>();
        for (Project project : projects) {
            result.add(toDto(project));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> findByEvent(Long eventId) {
        List<Project> projects = projectRepository.findByEventId(eventId);
        List<ProjectDto> result = new ArrayList<>();
        for (Project project : projects) {
            result.add(toDto(project));
        }
        return result;
    }

    public List<ProjectDto> findByCategory(Long categoryId) {
        List<Project> projects = projectRepository.findByCategoryId(categoryId);
        List<ProjectDto> result = new ArrayList<>();
        for (Project project : projects) {
            result.add(toDto(project));
        }
        return result;
    }

    @Transactional
    public ProjectDto createForEvent(Long eventId, ProjectDto dto) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));
        Project project = new Project(dto.getName(), dto.getDescription(), event);
        if (dto.getCategoryId() != null) {
            categoryRepository.findById(dto.getCategoryId()).ifPresent(project::setCategory);
        }
        return toDto(projectRepository.save(project));
    }

    @Transactional
    public ProjectDto addCompetitor(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        if (project.getCategory() == null) {
            throw new RuntimeException("El proyecto debe tener una categoría antes de añadir competidores");
        }

        project.getCompetitors().add(user);
        Project saved = projectRepository.save(project);

        Event event = project.getEvent();
        Category projectCategory = project.getCategory();
        if (!projectCategory.getEvent().getId().equals(event.getId())) {
            throw new RuntimeException("La categoría del proyecto no pertenece al evento del proyecto");
        }

        eventParticipationService.ensureCompetitorRegistration(event.getId(), user.getId(), projectCategory.getId());

        return toDto(saved);
    }

    public CommentDto addComment(Long projectId, CommentDto dto) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));

        User voter = userRepository.findById(dto.getVoterId())
                .orElseThrow(() -> new RuntimeException("Voter not found with id: " + dto.getVoterId()));

        Comment comment = new Comment();
        comment.setText(dto.getText());
        comment.setProject(project);
        comment.setVoter(voter);

        Comment saved = commentRepository.save(comment);
        return new CommentDto(saved.getId(), saved.getVoter().getId(), saved.getText());
    }

    //Listar Comentarios de un Proyecto  
    public List<CommentDto> getCommentsByProject(Long projectId) {
        // Validar que el proyecto existe
        if (!projectRepository.existsById(projectId)) {
            throw new RuntimeException("Proyecto no encontrado con id: " + projectId);
        }
        List<CommentDto> result = new ArrayList<>();

        // Fuente 1: tabla comments (legacy, actualmente vacía)
        List<Comment> comments = commentRepository.findByProjectId(projectId);
        for (Comment c : comments) {
            Long voterId = c.getVoter() != null ? c.getVoter().getId() : null;
            String voterName = c.getVoter() != null ? c.getVoter().getName() : null;
            result.add(new CommentDto(c.getId(), voterId, voterName, c.getText()));
        }

        // Fuente 2: campo comentario en votings (ADR-009 — fuente real de comentarios)
        List<Voting> votingsWithComment = votingRepository.findByProjectIdAndComentarioIsNotNull(projectId);
        for (Voting v : votingsWithComment) {
            Long voterId = v.getVoter() != null ? v.getVoter().getId() : null;
            String voterName = v.getVoter() != null ? v.getVoter().getName() : null;
            // IDs negativos para evitar colisión con IDs de la tabla comments
            result.add(new CommentDto(-v.getId(), voterId, voterName, v.getComentario()));
        }

        return result;
    }

    public List<Long> getCompetitorIds(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));
        List<Long> result = new ArrayList<>();
        for (User c : project.getCompetitors()) {
            result.add(c.getId());
        }
        return result;
    }

    public List<CompetitorDto> getCompetitors(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));
        List<CompetitorDto> result = new ArrayList<>();
        for (User c : project.getCompetitors()) {
            result.add(new CompetitorDto(c.getId(), c.getName(), c.getEmail())); // CompetitorDto kept for API compatibility
        }
        return result;
    }

    public ProjectFinalScoreDto getProjectScore(Long projectId, Long categoryId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));

        List<CategoryCriterionPoints> weights = criterionPointsRepository.findByCategoryId(categoryId);
        // Los votos se vinculan directamente al proyecto (project_id), no a competidores individuales
        List<Voting> votings = votingRepository.findByProjectIdInAndCategoryId(List.of(projectId), categoryId);

        Map<Long, Double> scoresByCriterion = new HashMap<>();
        for (Voting v : votings) {
            Long critId = v.getCriterion().getId();
            // Usar weighted_score si está disponible; si no, usar score
            double score = v.getWeightedScore() != null ? v.getWeightedScore() : 
                          (v.getScore() != null ? v.getScore().doubleValue() : 0.0);
            scoresByCriterion.merge(critId, score, Double::sum);
        }

        double finalScore = 0;
        int maxScore = 0;
        List<ProjectFinalScoreDto.CriterionScoreDetail> details = new ArrayList<>();

        for (CategoryCriterionPoints ccp : weights) {
            Long critId = ccp.getCriterion().getId();
            double score = scoresByCriterion.getOrDefault(critId, 0.0);
            int weight = ccp.getWeightPercent();
            finalScore += score;
            maxScore += weight;
            details.add(new ProjectFinalScoreDto.CriterionScoreDetail(
                    critId, ccp.getCriterion().getName(), (int) score, weight));
        }

        return new ProjectFinalScoreDto(projectId, project.getName(), categoryId, (int) finalScore, maxScore, details);
    }

    private ProjectDto toDto(Project project) {
        List<Long> competitorIds = new ArrayList<>();
        for (User c : project.getCompetitors()) {
            competitorIds.add(c.getId());
        }

        ProjectDto dto = new ProjectDto(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getEvent().getId(),
                competitorIds
        );
        if (project.getCategory() != null) {
            dto.setCategoryId(project.getCategory().getId());
        }
        return dto;
    }
}

