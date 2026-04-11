package com.votify.service;

import com.votify.dto.CompetitorDto;
import com.votify.dto.CommentDto;
import com.votify.dto.ProjectDto;
import com.votify.entity.*;
import com.votify.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final EventRepository eventRepository;
    private final CompetitorRepository competitorRepository;
    private final VoterRepository voterRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventParticipationRepository eventParticipationRepository;

    public ProjectService(ProjectRepository projectRepository,
                          EventRepository eventRepository,
                          CompetitorRepository competitorRepository,
                          VoterRepository voterRepository,
                          CommentRepository commentRepository,
                          UserRepository userRepository,
                          EventParticipationRepository eventParticipationRepository) {
        this.projectRepository = projectRepository;
        this.eventRepository = eventRepository;
        this.competitorRepository = competitorRepository;
        this.voterRepository = voterRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.eventParticipationRepository = eventParticipationRepository;
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

    /**
     * Crea un proyecto en un evento.
     * El usuario creador (creatorUserId) pasa automáticamente a ser Competitor
     * y queda como primer miembro del proyecto.
     * Si el dto no incluye creatorUserId se lanza excepción.
     */
    @Transactional
    public ProjectDto createForEvent(Long eventId, ProjectDto dto) {
        if (dto.getCreatorUserId() == null) {
            throw new RuntimeException("El creador del proyecto es obligatorio (creatorUserId)");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));

        User user = userRepository.findById(dto.getCreatorUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + dto.getCreatorUserId()));

        // Si el usuario aún no es Competitor, se convierte en uno
        Competitor competitor = competitorRepository.findByEmail(user.getEmail())
                .orElseGet(() -> competitorRepository.save(new Competitor(user.getName(), user.getEmail())));

        // Crear el proyecto con el competidor ya dentro
        Project project = new Project(dto.getName(), dto.getDescription(), event);
        project.getCompetitors().add(competitor);
        Project saved = projectRepository.save(project);

        // Registrar EventParticipation(role=COMPETITOR) si no existe ya en alguna categoría del evento
        List<Category> categories = event.getCategories();
        if (!categories.isEmpty()) {
            Category firstCategory = categories.get(0);
            boolean alreadyRegistered = eventParticipationRepository
                    .existsByEventIdAndUserIdAndCategoryId(eventId, competitor.getId(), firstCategory.getId());
            if (!alreadyRegistered) {
                eventParticipationRepository.save(
                        new EventParticipation(event, competitor, firstCategory, ParticipationRole.COMPETITOR));
            }
        }

        return toDto(saved);
    }

    /**
     * Añade un usuario existente a un proyecto como competidor.
     * Si el usuario aún no es Competitor, se convierte en uno automáticamente.
     */
    @Transactional
    public ProjectDto addCompetitor(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Si el usuario aún no es Competitor, se convierte en uno
        Competitor competitor = competitorRepository.findByEmail(user.getEmail())
                .orElseGet(() -> competitorRepository.save(new Competitor(user.getName(), user.getEmail())));

        project.getCompetitors().add(competitor);
        Project saved = projectRepository.save(project);

        // Registrar EventParticipation(role=COMPETITOR) si no existe ya
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
