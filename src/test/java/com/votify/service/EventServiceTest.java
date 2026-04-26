package com.votify.service;

import com.votify.dto.EventDto;
import com.votify.entity.Event;
import com.votify.entity.User;
import com.votify.persistence.CategoryCriterionPointsRepository;
import com.votify.persistence.CategoryRepository;
import com.votify.persistence.CommentRepository;
import com.votify.persistence.EvaluacionRepository;
import com.votify.persistence.EventJuryRepository;
import com.votify.persistence.EventParticipationRepository;
import com.votify.persistence.EventRepository;
import com.votify.persistence.ProjectRepository;
import com.votify.persistence.UserRepository;
import com.votify.persistence.VotingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
@DisplayName("EventService - Tests unitarios")
class EventServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private UserRepository userRepository;
    @Mock private EventParticipationRepository eventParticipationRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private VotingRepository votingRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private CategoryCriterionPointsRepository criterionPointsRepository;
    @Mock private EvaluacionRepository evaluacionRepository;
    @Mock private EventJuryRepository eventJuryRepository;

    private EventService eventService;

    private Event event1;
    private Event event2;

    @BeforeEach
    void setUp() {
        EventParticipationService eventParticipationService = new EventParticipationService(
                eventParticipationRepository,
                eventRepository,
                userRepository,
                categoryRepository,
                null
        );
        eventService = new EventService(
                eventRepository,
                eventParticipationService,
                userRepository,
                votingRepository,
                eventParticipationRepository,
                commentRepository,
                projectRepository,
                criterionPointsRepository,
                evaluacionRepository,
                eventJuryRepository
        );

        event1 = new Event("Hackathon 2026");
        event1.setId(1L);
        event1.setTimeInitial(new Date(1_000_000L));
        event1.setTimeFinal(new Date(9_000_000L));

        event2 = new Event("Demo Day");
        event2.setId(2L);
    }

    @Test
    @DisplayName("findAll -> retorna todos los eventos como DTOs")
    void findAll_returnsAllEventsAsDtos() {
        when(eventRepository.findAll()).thenReturn(List.of(event1, event2));

        List<EventDto> result = eventService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(EventDto::getName)
                .containsExactly("Hackathon 2026", "Demo Day");
    }

    @Test
    @DisplayName("findAll -> retorna lista vacia cuando no hay eventos")
    void findAll_returnsEmptyList_whenNoEvents() {
        when(eventRepository.findAll()).thenReturn(List.of());
        assertThat(eventService.findAll()).isEmpty();
    }

    @Test
    @DisplayName("findById -> retorna DTO del evento correcto")
    void findById_returnsCorrectDto() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event1));

        EventDto result = eventService.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Hackathon 2026");
    }

    @Test
    @DisplayName("findById -> lanza excepcion cuando el evento no existe")
    void findById_throwsException_whenNotFound() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.findById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("createForOrganizer -> crea evento asociado al organizador")
    void createForOrganizer_createsEventWithOrganizer() {
        User organizer = new User("Org", "org@test.com", null);
        organizer.setId(10L);

        Event saved = new Event("Nuevo Evento");
        saved.setId(5L);
        saved.setOrganizer(organizer);

        when(userRepository.findById(10L)).thenReturn(Optional.of(organizer));
        when(eventRepository.save(any(Event.class))).thenReturn(Objects.requireNonNull(saved));

        EventDto dto = new EventDto();
        dto.setName("Nuevo Evento");
        dto.setOrganizerId(10L);

        EventDto result = eventService.createForOrganizer(10L, dto);

        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getName()).isEqualTo("Nuevo Evento");
        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    @DisplayName("createForOrganizer -> lanza excepcion si el organizador no existe")
    void createForOrganizer_throwsException_whenOrganizerNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        EventDto dto = new EventDto();
        dto.setName("Test");
        dto.setOrganizerId(999L);

        assertThatThrownBy(() -> eventService.createForOrganizer(999L, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("update -> actualiza nombre y fechas del evento")
    void update_updatesEventNameAndDates() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event1));
        Event updated = new Event("Hackathon 2027");
        updated.setId(1L);
        when(eventRepository.save(any(Event.class))).thenReturn(Objects.requireNonNull(updated));

        EventDto dto = new EventDto();
        dto.setName("Hackathon 2027");

        EventDto result = eventService.update(1L, dto);

        assertThat(result.getName()).isEqualTo("Hackathon 2027");
    }

    @Test
    @DisplayName("delete -> llama a delete una sola vez")
    void delete_callsDeleteOnce() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event1));
        doNothing().when(eventRepository).delete(Objects.requireNonNull(event1));

        eventService.delete(1L, 10L);

        verify(eventRepository, times(1)).delete(Objects.requireNonNull(event1));
    }

    @Test
    @DisplayName("delete -> lanza excepcion para id inexistente")
    void delete_throwsException_whenNotFound() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.delete(99L, 10L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }
}
