package com.votify.service;

import com.votify.dto.ProjectDto;
import com.votify.dto.CommentDto;
import com.votify.entity.Comment;
import com.votify.entity.Competitor;
import com.votify.entity.Event;
import com.votify.entity.Project;
import com.votify.entity.Voter;
import com.votify.persistence.CommentRepository;
import com.votify.persistence.CompetitorRepository;
import com.votify.persistence.EventRepository;
import com.votify.persistence.ProjectRepository;
import com.votify.persistence.VoterRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final EventRepository eventRepository;
    private final CompetitorRepository competitorRepository;
    private final VoterRepository voterRepository;
    private final CommentRepository commentRepository;

    public ProjectService(ProjectRepository projectRepository,
                          EventRepository eventRepository,
                          CompetitorRepository competitorRepository,
                          VoterRepository voterRepository,
                          CommentRepository commentRepository) {
        this.projectRepository = projectRepository;
        this.eventRepository = eventRepository;
        this.competitorRepository = competitorRepository;
        this.voterRepository = voterRepository;
        this.commentRepository = commentRepository;
    }

    public List<ProjectDto> findAll() {
        return projectRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<ProjectDto> findByEvent(Long eventId) {
        return projectRepository.findByEventId(eventId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public CommentDto addComment(Long projectId, CommentDto dto) {
        if (projectId == null) throw new RuntimeException("Project ID cannot be null");
        Project project = projectRepository.findById(Objects.requireNonNull(projectId))
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));

        if (dto.getVoterId() == null) throw new RuntimeException("Voter ID cannot be null");
        Voter voter = voterRepository.findById(Objects.requireNonNull(dto.getVoterId()))
                .orElseThrow(() -> new RuntimeException("Voter not found with id: " + dto.getVoterId()));

        Comment comment = new Comment();
        comment.setText(dto.getText());
        comment.setProject(project);
        comment.setVoter(voter);

        Comment saved = commentRepository.save(Objects.requireNonNull(comment));

        return new CommentDto(saved.getId(), saved.getVoter().getId(), saved.getText());
    }

    public ProjectDto createForEvent(Long eventId, ProjectDto dto) {
        if (eventId == null) throw new RuntimeException("Event ID cannot be null");
        Event event = eventRepository.findById(Objects.requireNonNull(eventId))
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));

        Project project = new Project();
        project.setName(dto.getName());
        project.setDescription(dto.getDescription());
        project.setEvent(event);

        Project saved = projectRepository.save(Objects.requireNonNull(project));
        return toDto(saved);
    }

    public ProjectDto createForParticipantInEvent(Long participantId, Long eventId, ProjectDto dto) {
        if (eventId == null) throw new RuntimeException("Event ID cannot be null");
        Event event = eventRepository.findById(Objects.requireNonNull(eventId))
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));

        if (participantId == null) throw new RuntimeException("Participant ID cannot be null");
        Competitor competitor = competitorRepository.findById(Objects.requireNonNull(participantId))
                .orElseThrow(() -> new RuntimeException("Competitor not found with id: " + participantId));

        Project project = competitor.createProjectForEvent(dto.getName(), dto.getDescription(), event);
        Project saved = projectRepository.save(Objects.requireNonNull(project));
        return toDto(saved);
    }

    public ProjectDto addCompetitor(Long projectId, Long competitorId) {
        if (projectId == null) throw new RuntimeException("Project ID cannot be null");
        Project project = projectRepository.findById(Objects.requireNonNull(projectId))
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));
        
        if (competitorId == null) throw new RuntimeException("Competitor ID cannot be null");
        Competitor competitor = competitorRepository.findById(Objects.requireNonNull(competitorId))
                .orElseThrow(() -> new RuntimeException("Competitor not found with id: " + competitorId));

        project.getCompetitors().add(competitor);
        Project saved = projectRepository.save(Objects.requireNonNull(project));
        return toDto(saved);
    }

    public List<CommentDto> getCommentsByProject(Long projectId) {
        if (projectId == null) throw new RuntimeException("Project ID cannot be null");
        if (!projectRepository.existsById(Objects.requireNonNull(projectId))) {
            throw new RuntimeException("Project not found with id: " + projectId);
        }
        return commentRepository.findByProjectId(projectId).stream()
                .map(c -> new CommentDto(c.getId(), c.getVoter().getId(), c.getText()))
                .collect(Collectors.toList());
    }

    public List<Long> getCompetitorIds(Long projectId) {
        if (projectId == null) throw new RuntimeException("Project ID cannot be null");
        Project project = projectRepository.findById(Objects.requireNonNull(projectId))
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));
        return project.getCompetitors().stream()
                .map(Competitor::getId)
                .collect(Collectors.toList());
    }

    private ProjectDto toDto(Project project) {
        List<Long> competitorIds = project.getCompetitors().stream()
                .map(Competitor::getId)
                .collect(Collectors.toList());

        return new ProjectDto(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getEvent().getId(),
                competitorIds
        );
    }
}

