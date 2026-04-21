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
    private final EventParticipationRepository eventParticipationRepository;
    private final CategoryCriterionPointsRepository criterionPointsRepository;
    private final VotingRepository votingRepository;

    public ProjectService(ProjectRepository projectRepository,
                          EventRepository eventRepository,
                          CategoryRepository categoryRepository,
                          CommentRepository commentRepository,
                          UserRepository userRepository,
                          EventParticipationRepository eventParticipationRepository,
                          CategoryCriterionPointsRepository criterionPointsRepository,
                          VotingRepository votingRepository) {
        this.projectRepository = projectRepository;
        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.eventParticipationRepository = eventParticipationRepository;
        this.criterionPointsRepository = criterionPointsRepository;
        this.votingRepository = votingRepository;
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

        project.getCompetitors().add(user);
        Project saved = projectRepository.save(project);

        Event event = project.getEvent();
        List<Category> categories = event.getCategories();
        if (!categories.isEmpty()) {
            Category firstCategory = categories.get(0);
            boolean alreadyRegistered = eventParticipationRepository
                    .existsByEventIdAndUserIdAndCategoryId(event.getId(), user.getId(), firstCategory.getId());
            if (!alreadyRegistered) {
                eventParticipationRepository.save(
                        new EventParticipation(event, user, firstCategory, ParticipationRole.COMPETITOR));
            }
        }

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

    public List<CommentDto> getCommentsByProject(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new RuntimeException("Proyecto no encontrado con id: " + projectId);
        }
        List<Comment> comments = commentRepository.findByProjectId(projectId);
        List<CommentDto> result = new ArrayList<>();
        for (Comment c : comments) {
            Long voterId = c.getVoter() != null ? c.getVoter().getId() : null;
            result.add(new CommentDto(c.getId(), voterId, c.getText()));
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

        List<Long> competitorIds = new ArrayList<>();
        for (User c : project.getCompetitors()) {
            competitorIds.add(c.getId());
        }

        List<CategoryCriterionPoints> weights = criterionPointsRepository.findByCategoryId(categoryId);
        List<Voting> votings = competitorIds.isEmpty()
                ? new ArrayList<>()
                : votingRepository.findByCompetitorIdInAndCategoryId(competitorIds, categoryId);

        Map<Long, Integer> scoresByCriterion = new HashMap<>();
        for (Voting v : votings) {
            Long critId = v.getCriterion().getId();
            int score = v.getScore() != null ? v.getScore() : 0;
            scoresByCriterion.merge(critId, score, Integer::sum);
        }

        int finalScore = 0;
        int maxScore = 0;
        List<ProjectFinalScoreDto.CriterionScoreDetail> details = new ArrayList<>();

        for (CategoryCriterionPoints ccp : weights) {
            Long critId = ccp.getCriterion().getId();
            int score = scoresByCriterion.getOrDefault(critId, 0);
            int weight = ccp.getWeightPercent();
            finalScore += score;
            maxScore += weight;
            details.add(new ProjectFinalScoreDto.CriterionScoreDetail(
                    critId, ccp.getCriterion().getName(), score, weight));
        }

        return new ProjectFinalScoreDto(projectId, project.getName(), categoryId, finalScore, maxScore, details);
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
