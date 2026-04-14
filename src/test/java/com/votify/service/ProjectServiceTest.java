package com.votify.service;

import com.votify.dto.CommentDto;
import com.votify.dto.ProjectDto;
import com.votify.entity.*;
import com.votify.persistence.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectService — Tests unitarios")
class ProjectServiceTest {

    @Mock private ProjectRepository                    projectRepository;
    @Mock private EventRepository                      eventRepository;
    @Mock private CompetitorRepository                 competitorRepository;
    @Mock private VoterRepository                      voterRepository;
    @Mock private CommentRepository                    commentRepository;
    @Mock private UserRepository                       userRepository;
    @Mock private EventParticipationRepository         eventParticipationRepository;
    @Mock private CategoryCriterionPointsRepository    criterionPointsRepository;
    @Mock private VotingRepository                     votingRepository;

    @InjectMocks
    private ProjectService projectService;

    private Event      event;
    private Project    project;
    private Voter      voter;
    private Competitor competitor;

    @BeforeEach
    void setUp() {
        event = new Event("Hackathon 2026");
        event.setId(1L);

        project = new Project("EcoTrack", "App de carbono", event);
        project.setId(10L);

        voter = new Voter("Jurado1", "jurado@test.com");
        voter.setId(5L);

        competitor = new Competitor("Carlos", "carlos@test.com");
        competitor.setId(2L);
    }

    @Test
    @DisplayName("findByEvent → retorna proyectos del evento")
    void findByEvent_returnsProjectsOfEvent() {
        Project p2 = new Project("MediAI", "IA médica", event);
        p2.setId(11L);
        when(projectRepository.findByEventId(1L)).thenReturn(List.of(project, p2));

        List<ProjectDto> result = projectService.findByEvent(1L);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ProjectDto::getName)
                .containsExactlyInAnyOrder("EcoTrack", "MediAI");
    }

    @Test
    @DisplayName("findByEvent → retorna vacío si no hay proyectos")
    void findByEvent_returnsEmpty_whenNoProjects() {
        when(projectRepository.findByEventId(1L)).thenReturn(List.of());
        assertThat(projectService.findByEvent(1L)).isEmpty();
    }

    @Test
    @DisplayName("createForEvent → crea proyecto asociado al evento")
    void createForEvent_createsProject() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(projectRepository.save(any(Project.class))).thenReturn(Objects.requireNonNull(project));

        ProjectDto dto = new ProjectDto(null, "EcoTrack", "App de carbono", 1L, null);
        ProjectDto result = projectService.createForEvent(1L, dto);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getName()).isEqualTo("EcoTrack");
        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    @DisplayName("createForEvent → lanza excepción si el evento no existe")
    void createForEvent_throwsException_whenEventNotFound() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        ProjectDto dto = new ProjectDto(null, "Test", "Desc", 99L, null);
        assertThatThrownBy(() -> projectService.createForEvent(99L, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("addComment → guarda comentario correctamente")
    void addComment_savesComment() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(voterRepository.findById(5L)).thenReturn(Optional.of(voter));

        Comment savedComment = new Comment("Muy buena idea", voter, project);
        savedComment.setId(50L);
        when(commentRepository.save(any(Comment.class))).thenReturn(Objects.requireNonNull(savedComment));

        CommentDto dto = new CommentDto(null, 5L, "Muy buena idea");
        CommentDto result = projectService.addComment(10L, dto);

        assertThat(result.getId()).isEqualTo(50L);
        assertThat(result.getText()).isEqualTo("Muy buena idea");
        assertThat(result.getVoterId()).isEqualTo(5L);
        verify(commentRepository, times(1)).save(any(Comment.class));
    }

    @Test
    @DisplayName("addComment → lanza excepción si el proyecto no existe")
    void addComment_throwsException_whenProjectNotFound() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.addComment(99L, new CommentDto(null, 5L, "Texto")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("addComment → lanza excepción si el votante no existe")
    void addComment_throwsException_whenVoterNotFound() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(voterRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.addComment(10L, new CommentDto(null, 99L, "Texto")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Voter");
    }

    @Test
    @DisplayName("addCompetitor → asocia competidor al proyecto")
    void addCompetitor_linksCompetitorToProject() {
        User user = new User("Carlos", "carlos@test.com");
        user.setId(2L);

        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(competitorRepository.findByEmail("carlos@test.com")).thenReturn(Optional.of(competitor));
        when(projectRepository.save(any(Project.class))).thenReturn(Objects.requireNonNull(project));

        projectService.addCompetitor(10L, 2L);

        assertThat(project.getCompetitors()).contains(competitor);
        verify(projectRepository, times(1)).save(Objects.requireNonNull(project));
    }

    @Test
    @DisplayName("addCompetitor → lanza excepción si el usuario no existe")
    void addCompetitor_throwsException_whenUserNotFound() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.addCompetitor(10L, 99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }
}
