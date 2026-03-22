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

    public ProjectDto createForEvent(Long eventId, ProjectDto dto) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));

        Project project = new Project();
        project.setName(dto.getName());
        project.setDescription(dto.getDescription());
        project.setEvent(event);

        Project saved = projectRepository.save(project);
        return toDto(saved);
    }

    public ProjectDto createForParticipantInEvent(Long participantId, Long eventId, ProjectDto dto) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));

        Competitor competitor = competitorRepository.findById(participantId)
                .orElseThrow(() -> new RuntimeException("Competitor not found with id: " + participantId));

        Project project = competitor.createProjectForEvent(dto.getName(), dto.getDescription(), event);
        Project saved = projectRepository.save(project);
        return toDto(saved);
    }

    public ProjectDto addCompetitor(Long projectId, Long competitorId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));
        Competitor competitor = competitorRepository.findById(competitorId)
                .orElseThrow(() -> new RuntimeException("Competitor not found with id: " + competitorId));

        project.getCompetitors().add(competitor);
        Project saved = projectRepository.save(project);
        return toDto(saved);
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

