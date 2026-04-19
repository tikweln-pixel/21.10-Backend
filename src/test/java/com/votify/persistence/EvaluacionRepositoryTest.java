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

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("EvaluacionRepository — Tests de persistencia (SINGLE_TABLE)")
class EvaluacionRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private EvaluacionRepository evaluacionRepository;

    private User evaluador;
    private Competitor competitor;
    private Category category;
    private Event event;

    @BeforeEach
    void setUp() {
        event = new Event("Hackathon Test");
        em.persist(event);

        evaluador = new User("Admin", "admin@test.com", null);
        em.persist(evaluador);

        competitor = new Competitor("Carlos", "carlos@test.com", null);
        em.persist(competitor);

        category = new Category("Proyectos", event);
        category.setVotingType(VotingType.JURY_EXPERT);
        em.persist(category);

        em.flush();
    }

    @Test
    @DisplayName("save + findById → persiste EvaluacionNumerica via SINGLE_TABLE")
    void save_numerica_persistsCorrectly() {
        EvaluacionNumerica eval = new EvaluacionNumerica(evaluador, competitor, category, null, 1.0, "{\"valores\":[8,7]}");
        em.persist(eval);
        em.flush();
        em.clear();

        Evaluacion loaded = evaluacionRepository.findById(eval.getId()).orElseThrow();

        assertThat(loaded).isInstanceOf(EvaluacionNumerica.class);
        assertThat(loaded.calcularScore()).isEqualTo(15.0);
    }

    @Test
    @DisplayName("save + findById → persiste EvaluacionChecklist via SINGLE_TABLE")
    void save_checklist_persistsCorrectly() {
        EvaluacionChecklist eval = new EvaluacionChecklist(evaluador, competitor, category, null, 1.0, "{\"items\":[true,true,false]}");
        em.persist(eval);
        em.flush();
        em.clear();

        Evaluacion loaded = evaluacionRepository.findById(eval.getId()).orElseThrow();

        assertThat(loaded).isInstanceOf(EvaluacionChecklist.class);
        assertThat(loaded.calcularScore()).isCloseTo(66.67, within(0.01));
    }

    @Test
    @DisplayName("save + findById → persiste EvaluacionComentario (score null)")
    void save_comentario_persistsCorrectly() {
        EvaluacionComentario eval = new EvaluacionComentario(evaluador, competitor, category, null, 0.0, "{\"texto\":\"Buen trabajo\"}");
        em.persist(eval);
        em.flush();
        em.clear();

        Evaluacion loaded = evaluacionRepository.findById(eval.getId()).orElseThrow();

        assertThat(loaded).isInstanceOf(EvaluacionComentario.class);
        assertThat(loaded.calcularScore()).isNull();
    }

    @Test
    @DisplayName("findByCategoryId → retorna evaluaciones de la categoría")
    void findByCategoryId_returnsFiltered() {
        EvaluacionNumerica e1 = new EvaluacionNumerica(evaluador, competitor, category, null, 1.0, "{\"valores\":[5]}");
        em.persist(e1);

        Category otherCategory = new Category("Otra", event);
        otherCategory.setVotingType(VotingType.POPULAR_VOTE);
        em.persist(otherCategory);

        EvaluacionNumerica e2 = new EvaluacionNumerica(evaluador, competitor, otherCategory, null, 1.0, "{\"valores\":[3]}");
        em.persist(e2);
        em.flush();

        List<Evaluacion> result = evaluacionRepository.findByCategoryId(category.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).calcularScore()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("findByCompetitorId → retorna evaluaciones del competidor")
    void findByCompetitorId_returnsFiltered() {
        EvaluacionRubrica e1 = new EvaluacionRubrica(evaluador, competitor, category, null, 1.0,
                "{\"niveles\":[{\"nivel\":4,\"max\":5}]}");
        em.persist(e1);
        em.flush();

        List<Evaluacion> result = evaluacionRepository.findByCompetitorId(competitor.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isInstanceOf(EvaluacionRubrica.class);
    }

    @Test
    @DisplayName("findByCategoryIdAndCompetitorId → filtra por ambos")
    void findByCategoryAndCompetitor_returnsFiltered() {
        EvaluacionNumerica e1 = new EvaluacionNumerica(evaluador, competitor, category, null, 1.0, "{\"valores\":[10]}");
        em.persist(e1);
        em.flush();

        List<Evaluacion> result = evaluacionRepository.findByCategoryIdAndCompetitorId(
                category.getId(), competitor.getId());

        assertThat(result).hasSize(1);
    }
}
