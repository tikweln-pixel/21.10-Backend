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
import java.util.stream.Collectors;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final CompetitorRepository competitorRepository;
    private final VoterRepository voterRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventParticipationRepository eventParticipationRepository;
    private final CategoryCriterionPointsRepository criterionPointsRepository;
    private final VotingRepository votingRepository;

    public ProjectService(ProjectRepository projectRepository,
                          EventRepository eventRepository,
                          CategoryRepository categoryRepository,
                          CompetitorRepository competitorRepository,
                          VoterRepository voterRepository,
                          CommentRepository commentRepository,
                          UserRepository userRepository,
                          EventParticipationRepository eventParticipationRepository,
                          CategoryCriterionPointsRepository criterionPointsRepository,
                          VotingRepository votingRepository) {
        this.projectRepository = projectRepository;
        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
        this.competitorRepository = competitorRepository;
        this.voterRepository = voterRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.eventParticipationRepository = eventParticipationRepository;
        this.criterionPointsRepository = criterionPointsRepository;
        this.votingRepository = votingRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> findAll() {
        return projectRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> findByEvent(Long eventId) {
        return projectRepository.findByEventId(eventId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<ProjectDto> findByCategory(Long categoryId) {
        return projectRepository.findByCategoryId(categoryId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjectDto createForEvent(Long eventId, ProjectDto dto) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));
        Project project = new Project(dto.getName(), dto.getDescription(), event);
        if (dto.getCategoryId() != null) {
            categoryRepository.findById(dto.getCategoryId())
                    .ifPresent(project::setCategory);
        }
        return toDto(projectRepository.save(project));
    }

    @Transactional
    public ProjectDto addCompetitor(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Competitor competitor = competitorRepository.findByEmail(user.getEmail())
                .orElseGet(() -> competitorRepository.save(new Competitor(user.getName(), user.getEmail())));

        project.getCompetitors().add(competitor);
        Project saved = projectRepository.save(project);

        Event event = project.getEvent();
        List<Category> categories = event.getCategories();
        if (!categories.isEmpty()) {
            Category firstCategory = categories.get(0);
            boolean alreadyRegistered = eventParticipationRepository
                    .existsByEventIdAndUserIdAndCategoryId(event.getId(), competitor.getId(), firstCategory.getId());
            if (!alreadyRegistered) {
                eventParticipationRepository.save(
                        new EventParticipation(event, competitor, firstCategory, ParticipationRole.COMPETITOR));
            }
        }

        return toDto(saved);
    }

    public CommentDto addComment(Long projectId, CommentDto dto) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));

        Voter voter = voterRepository.findById(dto.getVoterId())
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
            throw new RuntimeException("Project not found with id: " + projectId);
        }
        return commentRepository.findByProjectId(projectId).stream()
                .map(c -> new CommentDto(c.getId(), c.getVoter().getId(), c.getText()))
                .collect(Collectors.toList());
    }

    public List<Long> getCompetitorIds(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));
        return project.getCompetitors().stream()
                .map(Competitor::getId)
                .collect(Collectors.toList());
    }

    public List<CompetitorDto> getCompetitors(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));
        return project.getCompetitors().stream()
                .map(c -> new CompetitorDto(c.getId(), c.getName(), c.getEmail()))
                .collect(Collectors.toList());
    }

    public ProjectFinalScoreDto getProjectScore(Long projectId, Long categoryId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));

        List<Long> competitorIds = project.getCompetitors().stream()
                .map(Competitor::getId)
                .collect(Collectors.toList());

        List<CategoryCriterionPoints> weights = criterionPointsRepository.findByCategoryId(categoryId);
        List<Voting> votings = competitorIds.isEmpty()
                ? List.of()
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

        return new ProjectFinalScoreDto(projectId, project.getName(), categoryId,
                finalScore, maxScore, details);
    }

    private ProjectDto toDto(Project project) {
        List<Long> competitorIds = project.getCompetitors().stream()
                .map(Competitor::getId)
                .collect(Collectors.toList());

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
