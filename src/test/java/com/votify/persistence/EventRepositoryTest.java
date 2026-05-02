package com.votify.persistence;

import com.votify.entity.Event;
import com.votify.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests de persistencia para EventRepository.
 *
 * Patrón testado: Repository Pattern + Spring Data JPA derived query.
 * El método existsByOrganizerId() navega la relación @ManyToOne organizer.id
 * y es crítico para el checkAccess() de CompetitorController (BUG-9).
 *
 * Directorio GitHub: src/test/java/com/votify/persistence/EventRepositoryTest.java
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("EventRepository — Tests de persistencia")
class EventRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private EventRepository eventRepository;

    private User organizer;
    private User otherUser;
    private Event eventWithOrganizer;

    @BeforeEach
    void setUp() {
        organizer = new User("Organizador Test", "org@test.com", null);
        em.persist(organizer);

        otherUser = new User("Usuario Sin Evento", "otro@test.com", null);
        em.persist(otherUser);

        eventWithOrganizer = new Event("Hackathon 2026");
        eventWithOrganizer.setOrganizer(organizer);
        em.persist(eventWithOrganizer);

        em.flush();
    }

    // ── existsByOrganizerId ─────────────────────────────────────────────────

    @Test
    @DisplayName("existsByOrganizerId → true cuando el usuario es organizador de algún evento")
    void existsByOrganizerId_returnsTrue_whenUserIsOrganizer() {
        boolean result = eventRepository.existsByOrganizerId(organizer.getId());

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("existsByOrganizerId → false cuando el usuario no organiza ningún evento")
    void existsByOrganizerId_returnsFalse_whenUserIsNotOrganizer() {
        boolean result = eventRepository.existsByOrganizerId(otherUser.getId());

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("existsByOrganizerId → false para un ID que no existe en la base de datos")
    void existsByOrganizerId_returnsFalse_whenUserIdDoesNotExist() {
        boolean result = eventRepository.existsByOrganizerId(9999L);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("existsByOrganizerId → true aunque el organizador tenga varios eventos")
    void existsByOrganizerId_returnsTrue_whenUserOrganizesMultipleEvents() {
        Event secondEvent = new Event("Competición Primavera");
        secondEvent.setOrganizer(organizer);
        em.persist(secondEvent);
        em.flush();

        boolean result = eventRepository.existsByOrganizerId(organizer.getId());

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("existsByOrganizerId → false cuando existe un evento pero sin organizador asignado")
    void existsByOrganizerId_returnsFalse_whenEventHasNoOrganizer() {
        Event eventSinOrganizador = new Event("Evento Sin Responsable");
        // organizer es null (campo optional=true en la entidad)
        em.persist(eventSinOrganizador);
        em.flush();

        // otherUser no es organizador de ningún evento
        boolean result = eventRepository.existsByOrganizerId(otherUser.getId());

        assertThat(result).isFalse();
    }
}
