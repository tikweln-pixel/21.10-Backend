package com.votify.persistence;

import com.votify.entity.Event;
import com.votify.entity.Project;
import com.votify.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests de persistencia para ProjectRepository.
 *
 * Patrón testado: Repository Pattern + JPQL custom query con JOIN en @ManyToMany.
 * La query findByCompetitorId() recorre la tabla join project_competitors
 * y es crítica para HojaRutaMejoraService.recogerComentariosAdicionales() (BUG-9).
 *
 * Query probada:
 *   SELECT p FROM Project p JOIN p.competitors c WHERE c.id = :competitorId
 *
 * Directorio GitHub: src/test/java/com/votify/persistence/ProjectRepositoryTest.java
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ProjectRepository — Tests de persistencia (JPQL custom)")
class ProjectRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ProjectRepository projectRepository;

    private User competitor;
    private User notACompetitor;
    private Project project1;
    private Project project2;

    @BeforeEach
    void setUp() {
        competitor = new User("Competidor Test", "comp@test.com", null);
        em.persist(competitor);

        notACompetitor = new User("Usuario Sin Proyecto", "noproject@test.com", null);
        em.persist(notACompetitor);

        Event event = new Event("Hackathon 2026");
        em.persist(event);

        // Dos proyectos donde competitor es miembro del equipo
        project1 = new Project("EcoTrack", "App de huella ecológica", event);
        project1.getCompetitors().add(competitor);
        em.persist(project1);

        project2 = new Project("MediAI", "IA para diagnóstico médico", event);
        project2.getCompetitors().add(competitor);
        em.persist(project2);

        em.flush();
    }

    // ── findByCompetitorId ──────────────────────────────────────────────────

    @Test
    @DisplayName("findByCompetitorId → devuelve todos los proyectos donde el usuario es competidor")
    void findByCompetitorId_returnsProjectsWhereUserIsCompetitor() {
        List<Project> result = projectRepository.findByCompetitorId(competitor.getId());

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Project::getName)
                .containsExactlyInAnyOrder("EcoTrack", "MediAI");
    }

    @Test
    @DisplayName("findByCompetitorId → lista vacía cuando el usuario no pertenece a ningún proyecto")
    void findByCompetitorId_returnsEmpty_whenUserHasNoProjects() {
        List<Project> result = projectRepository.findByCompetitorId(notACompetitor.getId());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByCompetitorId → lista vacía para un ID que no existe en la base de datos")
    void findByCompetitorId_returnsEmpty_whenUserIdDoesNotExist() {
        List<Project> result = projectRepository.findByCompetitorId(9999L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByCompetitorId → no devuelve proyectos de otros competidores")
    void findByCompetitorId_doesNotReturnProjectsOfOtherCompetitors() {
        User otherCompetitor = new User("Otro Competidor", "other@test.com", null);
        em.persist(otherCompetitor);

        Event event2 = new Event("Demo Day 2026");
        em.persist(event2);

        Project projectAjeno = new Project("Proyecto Ajeno", "No debe aparecer", event2);
        projectAjeno.getCompetitors().add(otherCompetitor);
        em.persist(projectAjeno);
        em.flush();

        List<Project> result = projectRepository.findByCompetitorId(competitor.getId());

        assertThat(result).extracting(Project::getName)
                .doesNotContain("Proyecto Ajeno");
    }

    @Test
    @DisplayName("findByCompetitorId → un usuario en un único proyecto devuelve lista de tamaño 1")
    void findByCompetitorId_returnsSingleProject_whenUserBelongsToOne() {
        User soloCompetitor = new User("Solo Competitor", "solo@test.com", null);
        em.persist(soloCompetitor);

        Event event3 = new Event("Evento Individual");
        em.persist(event3);

        Project soloProject = new Project("Mi Proyecto", "Solo mío", event3);
        soloProject.getCompetitors().add(soloCompetitor);
        em.persist(soloProject);
        em.flush();

        List<Project> result = projectRepository.findByCompetitorId(soloCompetitor.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Mi Proyecto");
    }

    @Test
    @DisplayName("findByCompetitorId → un proyecto puede tener varios competidores sin afectar los resultados del otro")
    void findByCompetitorId_isolatesResultsByCompetitor() {
        User secondCompetitor = new User("Segundo Competidor", "second@test.com", null);
        em.persist(secondCompetitor);

        // Añadir secondCompetitor a project1 también
        project1.getCompetitors().add(secondCompetitor);
        em.persist(project1);
        em.flush();

        // competitor sigue viendo sus 2 proyectos
        assertThat(projectRepository.findByCompetitorId(competitor.getId())).hasSize(2);
        // secondCompetitor solo ve project1
        assertThat(projectRepository.findByCompetitorId(secondCompetitor.getId())).hasSize(1);
    }
}
