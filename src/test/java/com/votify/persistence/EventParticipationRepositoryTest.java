package com.votify.persistence;

import com.votify.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("EventParticipationRepository — Tests de persistencia")
class EventParticipationRepositoryTest {

    @Autowired TestEntityManager              em;
    @Autowired EventParticipationRepository   repo;

    private Event    event;
    private Category catJury, catPopular;
    private User     userComp, userVoter;

    @BeforeEach
    void setUp() {
        event = new Event("Hackathon 2026 Repo");
        em.persist(event);

        catJury = new Category("Jurado", event);
        em.persist(catJury);

        catPopular = new Category("Popular", event);
        em.persist(catPopular);

        userComp = new User("Comp User", "comp_part@test.com", null);
        em.persist(userComp);

        userVoter = new User("Spectator User", "spectator_part@test.com", null);
        em.persist(userVoter);

        // Competitor in Jury category
        EventParticipation p1 = new EventParticipation(event, userComp, catJury, ParticipationRole.COMPETITOR);
        em.persist(p1);

        // Spectator in Jury category
        EventParticipation p2 = new EventParticipation(event, userVoter, catJury, ParticipationRole.SPECTATOR);
        em.persist(p2);

        // Competitor in Popular category
        EventParticipation p3 = new EventParticipation(event, userComp, catPopular, ParticipationRole.COMPETITOR);
        em.persist(p3);

        em.flush();
    }

    // ── findByEventId ──────────────────────────────────────────────────────

    @Test
    @DisplayName("findByEventId → retorna todas las participaciones del evento")
    void findByEventId_returnsAllParticipationsOfEvent() {
        List<EventParticipation> result = repo.findByEventId(event.getId());
        assertThat(result).hasSize(3);
    }

    // ── findByEventIdAndCategoryId ─────────────────────────────────────────

    @Test
    @DisplayName("findByEventIdAndCategoryId → filtra por evento y categoría")
    void findByEventIdAndCategoryId_filtersCorrectly() {
        List<EventParticipation> result = repo.findByEventIdAndCategoryId(event.getId(), catJury.getId());
        assertThat(result).hasSize(2);
    }

    // ── findByEventIdAndCategoryIdAndRole ──────────────────────────────────

    @Test
    @DisplayName("findByEventIdAndCategoryIdAndRole → retorna solo competidores del jurado")
    void findByEventIdAndCategoryIdAndRole_returnsOnlyCompetitors() {
        List<EventParticipation> competitors = repo.findByEventIdAndCategoryIdAndRole(
                event.getId(), catJury.getId(), ParticipationRole.COMPETITOR);

        assertThat(competitors).hasSize(1);
        assertThat(competitors.get(0).getUser().getId()).isEqualTo(userComp.getId());
    }

    @Test
    @DisplayName("findByEventIdAndCategoryIdAndRole → retorna solo espectadores del jurado")
    void findByEventIdAndCategoryIdAndRole_returnsOnlySpectators() {
        List<EventParticipation> spectators = repo.findByEventIdAndCategoryIdAndRole(
                event.getId(), catJury.getId(), ParticipationRole.SPECTATOR);

        assertThat(spectators).hasSize(1);
        assertThat(spectators.get(0).getUser().getId()).isEqualTo(userVoter.getId());
    }

    // ── existsByEventIdAndUserIdAndCategoryId ─────────────────────────────

    @Test
    @DisplayName("existsByEventIdAndUserIdAndCategoryId → true si ya está registrado")
    void exists_returnsTrue_whenAlreadyRegistered() {
        boolean exists = repo.existsByEventIdAndUserIdAndCategoryId(
                event.getId(), userComp.getId(), catJury.getId());
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByEventIdAndUserIdAndCategoryId → false si no está registrado")
    void exists_returnsFalse_whenNotRegistered() {
        // userVoter no está en catPopular
        boolean exists = repo.existsByEventIdAndUserIdAndCategoryId(
                event.getId(), userVoter.getId(), catPopular.getId());
        assertThat(exists).isFalse();
    }

    // ── findByEventIdAndUserIdAndCategoryId ────────────────────────────────

    @Test
    @DisplayName("findByEventIdAndUserIdAndCategoryId → retorna participación exacta")
    void findByEventAndUserAndCategory_returnsCorrectParticipation() {
        Optional<EventParticipation> result = repo.findByEventIdAndUserIdAndCategoryId(
                event.getId(), userComp.getId(), catJury.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getRole()).isEqualTo(ParticipationRole.COMPETITOR);
    }

    // ── findByUserId ───────────────────────────────────────────────────────

    @Test
    @DisplayName("findByUserId → retorna todas las participaciones del usuario")
    void findByUserId_returnsAllParticipationsOfUser() {
        // userComp está en catJury y catPopular
        List<EventParticipation> result = repo.findByUserId(userComp.getId());
        assertThat(result).hasSize(2);
    }
}
