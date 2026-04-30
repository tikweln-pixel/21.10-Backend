package com.votify.service;

import com.votify.dto.EventParticipationDto;
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
@DisplayName("EventParticipationService — Tests unitarios")
class EventParticipationServiceTest {

    @Mock private EventParticipationRepository eventParticipationRepository;
    @Mock private EventRepository              eventRepository;
    @Mock private UserRepository               userRepository;
    @Mock private CategoryRepository           categoryRepository;
    @Mock private EventJuryRepository          eventJuryRepository;

    @InjectMocks
    private EventParticipationService service;

    private Event    event;
    private User     user;
    private Category category;

    @BeforeEach
    void setUp() {
        event = new Event("Hackathon 2026");
        event.setId(1L);

        user = new User("Carlos", "carlos@test.com", null);
        user.setId(2L);

        category = new Category("Jurado", event);
        category.setId(10L);
    }

    // ── registerParticipation ──────────────────────────────────────────────

    @Test
    @DisplayName("registerParticipation → registra competidor correctamente")
    void registerParticipation_registersCompetitor() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        EventParticipation saved = new EventParticipation(event, user, category, ParticipationRole.COMPETITOR);
        saved.setId(99L);
        when(eventParticipationRepository.save(any(EventParticipation.class))).thenReturn(Objects.requireNonNull(saved));
        when(categoryRepository.findByEventId(1L)).thenReturn(List.of());

        EventParticipationDto result = service.registerCompetitor(1L, 2L, 10L);

        assertThat(result.getRole()).isEqualTo(ParticipationRole.COMPETITOR);
        verify(eventParticipationRepository, times(1)).save(any(EventParticipation.class));
    }

    @Test
    @DisplayName("registerParticipation → registra espectador correctamente")
    void registerParticipation_registersSpectator() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(eventParticipationRepository.existsByEventIdAndUserIdAndCategoryId(1L, 2L, 10L)).thenReturn(false);

        EventParticipation saved = new EventParticipation(event, user, category, ParticipationRole.SPECTATOR);
        saved.setId(100L);
        when(eventParticipationRepository.save(any(EventParticipation.class))).thenReturn(Objects.requireNonNull(saved));

        EventParticipationDto result = service.registerParticipation(1L, 2L, 10L, ParticipationRole.SPECTATOR);

        assertThat(result.getRole()).isEqualTo(ParticipationRole.SPECTATOR);
    }

    @Test
    @DisplayName("registerParticipation → lanza excepción si categoría es null")
    void registerParticipation_throwsException_whenCategoryIsNull() {
        assertThatThrownBy(() -> service.registerParticipation(1L, 2L, null, ParticipationRole.COMPETITOR))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("categoría es obligatoria");
    }

    @Test
    @DisplayName("registerParticipation → lanza excepción si el usuario ya está registrado")
    void registerParticipation_throwsException_whenAlreadyRegistered() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(eventParticipationRepository.existsByEventIdAndUserIdAndCategoryId(1L, 2L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> service.registerParticipation(1L, 2L, 10L, ParticipationRole.COMPETITOR))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ya está registrado");
    }

    @Test
    @DisplayName("registerParticipation → lanza excepción si la categoría no pertenece al evento")
    void registerParticipation_throwsException_whenCategoryDoesNotBelongToEvent() {
        Event otherEvent = new Event("Otro Evento");
        otherEvent.setId(99L);
        Category wrongCategory = new Category("Otra Cat", otherEvent);
        wrongCategory.setId(10L);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(wrongCategory));

        assertThatThrownBy(() -> service.registerParticipation(1L, 2L, 10L, ParticipationRole.COMPETITOR))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no pertenece");
    }

    // ── getParticipations ──────────────────────────────────────────────────

    @Test
    @DisplayName("getParticipationsByEvent → retorna todas las participaciones del evento")
    void getParticipationsByEvent_returnsParticipations() {
        EventParticipation p1 = new EventParticipation(event, user, category, ParticipationRole.COMPETITOR);
        p1.setId(1L);
        when(eventParticipationRepository.findByEventId(1L)).thenReturn(List.of(p1));

        List<EventParticipationDto> result = service.getParticipationsByEvent(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRole()).isEqualTo(ParticipationRole.COMPETITOR);
    }

    // ── removeParticipation ────────────────────────────────────────────────

    @Test
    @DisplayName("removeParticipation → elimina participación existente")
    void removeParticipation_deletesParticipation() {
        EventParticipation p = new EventParticipation(event, user, category, ParticipationRole.COMPETITOR);
        p.setId(1L);
        when(eventParticipationRepository.findByEventIdAndUserIdAndCategoryId(1L, 2L, 10L))
                .thenReturn(Optional.of(p));
        doNothing().when(eventParticipationRepository).delete(p);

        service.removeParticipation(1L, 2L, 10L);

        verify(eventParticipationRepository, times(1)).delete(p);
    }

    @Test
    @DisplayName("removeParticipation → lanza excepción si la participación no existe")
    void removeParticipation_throwsException_whenNotFound() {
        when(eventParticipationRepository.findByEventIdAndUserIdAndCategoryId(1L, 2L, 10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeParticipation(1L, 2L, 10L))
                .isInstanceOf(RuntimeException.class);
    }

    // ── ensureSpectatorRegistrationInAllCategories ─────────────────────────

    @Test
    @DisplayName("ensureSpectatorRegistrationInAllCategories → crea spectator en todas las categorías faltantes")
    void ensureSpectatorRegistrationInAllCategories_createsMissingSpectators() {
        Category category2 = new Category("Publico", event);
        category2.setId(11L);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(categoryRepository.findByEventId(1L)).thenReturn(List.of(category, category2));
        when(eventParticipationRepository.findByEventIdAndUserIdAndCategoryId(1L, 2L, 10L)).thenReturn(Optional.empty());
        when(eventParticipationRepository.findByEventIdAndUserIdAndCategoryId(1L, 2L, 11L)).thenReturn(Optional.empty());
        when(eventParticipationRepository.save(any(EventParticipation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<EventParticipationDto> result = service.ensureSpectatorRegistrationInAllCategories(1L, 2L);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(EventParticipationDto::getRole)
                .containsExactly(ParticipationRole.SPECTATOR, ParticipationRole.SPECTATOR);
        verify(eventParticipationRepository, times(2)).save(any(EventParticipation.class));
    }

    @Test
    @DisplayName("ensureSpectatorRegistrationInAllCategories → conserva roles existentes y crea solo las faltantes")
    void ensureSpectatorRegistrationInAllCategories_keepsExistingRoles() {
        Category category2 = new Category("Publico", event);
        category2.setId(11L);

        EventParticipation existingCompetitor = new EventParticipation(event, user, category, ParticipationRole.COMPETITOR);
        existingCompetitor.setId(50L);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(categoryRepository.findByEventId(1L)).thenReturn(List.of(category, category2));
        when(eventParticipationRepository.findByEventIdAndUserIdAndCategoryId(1L, 2L, 10L))
                .thenReturn(Optional.of(existingCompetitor));
        when(eventParticipationRepository.findByEventIdAndUserIdAndCategoryId(1L, 2L, 11L))
                .thenReturn(Optional.empty());
        when(eventParticipationRepository.save(any(EventParticipation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<EventParticipationDto> result = service.ensureSpectatorRegistrationInAllCategories(1L, 2L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRole()).isEqualTo(ParticipationRole.COMPETITOR);
        assertThat(result.get(1).getRole()).isEqualTo(ParticipationRole.SPECTATOR);
        verify(eventParticipationRepository, times(1)).save(any(EventParticipation.class));
    }

    // ── registerAnonymousSpectator ─────────────────────────────────────────

    @Test
    @DisplayName("registerAnonymousSpectator → crea usuario nuevo y lo registra como SPECTATOR")
    void registerAnonymousSpectator_createsNewUserAndRegisters() {
        User newUser = new User("Espectador Anónimo", "anon_1@votify.local", "");
        newUser.setId(5L);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(userRepository.findByEmail("anon_1@votify.local")).thenReturn(null);
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        when(eventParticipationRepository.findByEventIdAndUserIdAndCategoryId(1L, 5L, 10L))
                .thenReturn(Optional.empty());
        when(eventParticipationRepository.save(any(EventParticipation.class)))
                .thenAnswer(inv -> {
                    EventParticipation p = inv.getArgument(0);
                    p.setId(200L);
                    return p;
                });

        EventParticipationDto result = service.registerAnonymousSpectator(1L, "Espectador Anónimo", "anon_1@votify.local", 10L);

        assertThat(result.getRole()).isEqualTo(ParticipationRole.SPECTATOR);
        assertThat(result.getUserId()).isEqualTo(5L);
        verify(userRepository, times(1)).save(any(User.class));
        verify(eventParticipationRepository, times(1)).save(any(EventParticipation.class));
    }

    @Test
    @DisplayName("registerAnonymousSpectator → reutiliza usuario existente por email, no lo duplica")
    void registerAnonymousSpectator_reusesExistingUserByEmail() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(userRepository.findByEmail("carlos@test.com")).thenReturn(user);
        when(eventParticipationRepository.findByEventIdAndUserIdAndCategoryId(1L, 2L, 10L))
                .thenReturn(Optional.empty());
        when(eventParticipationRepository.save(any(EventParticipation.class)))
                .thenAnswer(inv -> {
                    EventParticipation p = inv.getArgument(0);
                    p.setId(201L);
                    return p;
                });

        EventParticipationDto result = service.registerAnonymousSpectator(1L, "Carlos", "carlos@test.com", 10L);

        assertThat(result.getUserId()).isEqualTo(2L);
        assertThat(result.getRole()).isEqualTo(ParticipationRole.SPECTATOR);
        verify(userRepository, never()).save(any(User.class));
        verify(eventParticipationRepository, times(1)).save(any(EventParticipation.class));
    }

    @Test
    @DisplayName("registerAnonymousSpectator → idempotente: devuelve participación existente sin duplicar")
    void registerAnonymousSpectator_isIdempotent_whenAlreadyRegistered() {
        EventParticipation existing = new EventParticipation(event, user, category, ParticipationRole.SPECTATOR);
        existing.setId(99L);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(userRepository.findByEmail("carlos@test.com")).thenReturn(user);
        when(eventParticipationRepository.findByEventIdAndUserIdAndCategoryId(1L, 2L, 10L))
                .thenReturn(Optional.of(existing));

        EventParticipationDto result = service.registerAnonymousSpectator(1L, "Carlos", "carlos@test.com", 10L);

        assertThat(result.getId()).isEqualTo(99L);
        assertThat(result.getRole()).isEqualTo(ParticipationRole.SPECTATOR);
        verify(eventParticipationRepository, never()).save(any(EventParticipation.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("registerAnonymousSpectator → lanza excepción si eventId es null")
    void registerAnonymousSpectator_throwsException_whenEventIdIsNull() {
        assertThatThrownBy(() -> service.registerAnonymousSpectator(null, "Ana", "ana@test.com", 10L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("evento es obligatorio");
    }

    @Test
    @DisplayName("registerAnonymousSpectator → lanza excepción si categoryId es null")
    void registerAnonymousSpectator_throwsException_whenCategoryIdIsNull() {
        assertThatThrownBy(() -> service.registerAnonymousSpectator(1L, "Ana", "ana@test.com", null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("categoría es obligatorio");
    }

    @Test
    @DisplayName("registerAnonymousSpectator → lanza excepción si email es blank")
    void registerAnonymousSpectator_throwsException_whenEmailIsBlank() {
        assertThatThrownBy(() -> service.registerAnonymousSpectator(1L, "Ana", "  ", 10L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("email es obligatorio");
    }

    @Test
    @DisplayName("registerAnonymousSpectator → lanza excepción si la categoría no pertenece al evento")
    void registerAnonymousSpectator_throwsException_whenCategoryDoesNotBelongToEvent() {
        Event otherEvent = new Event("Otro Evento");
        otherEvent.setId(99L);
        Category wrongCategory = new Category("Otra Cat", otherEvent);
        wrongCategory.setId(10L);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(wrongCategory));

        assertThatThrownBy(() -> service.registerAnonymousSpectator(1L, "Ana", "ana@test.com", 10L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no pertenece");
    }
}
