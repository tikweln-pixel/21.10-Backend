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
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("VotingRepository — Tests de persistencia")
class VotingRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired VotingRepository  votingRepository;

    private User      voter;
    private Project   project1, project2;
    private Category  baseCategory;
    private Criterion criterion1, criterion2;
    private Voting    voting1, voting2;

    @BeforeEach
    void setUp() {
        voter = new User("Jurado Test", "jurado_repo@test.com", null);
        em.persist(voter);

        // Voting ahora se vincula a Project (no a User/competitor directamente)
        Event event = new Event("Hackathon Test");
        em.persist(event);

        baseCategory = new Category("General", event);
        em.persist(baseCategory);

        project1 = new Project("Proyecto Alpha", "Descripción Alpha", event);
        em.persist(project1);

        project2 = new Project("Proyecto Beta", "Descripción Beta", event);
        em.persist(project2);

        criterion1 = new Criterion("Innovación");
        criterion1.setCategory(baseCategory);
        em.persist(criterion1);

        criterion2 = new Criterion("Presentación");
        criterion2.setCategory(baseCategory);
        em.persist(criterion2);

        voting1 = new Voting(voter, project1, criterion1, 25);
        voting1.setCategory(baseCategory);
        em.persist(voting1);

        voting2 = new Voting(voter, project1, criterion2, 18);
        voting2.setCategory(baseCategory);
        em.persist(voting2);

        em.flush();
    }

    @Test
    @DisplayName("findAll → retorna todos los votos persistidos")
    void findAll_returnsAllVotings() {
        List<Voting> all = votingRepository.findAll();
        assertThat(all).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("findById → retorna voto correcto con todas sus relaciones")
    void findById_returnsVotingWithRelations() {
        Long vId = voting1.getId();
        Optional<Voting> result = votingRepository.findById(Objects.requireNonNull(vId));

        assertThat(result).isPresent();
        Voting v = result.get();
        assertThat(v.getScore()).isEqualTo(25);
        assertThat(v.getVoter().getId()).isEqualTo(voter.getId());
        assertThat(v.getProject().getId()).isEqualTo(project1.getId());
        assertThat(v.getCriterion().getId()).isEqualTo(criterion1.getId());
    }

    @Test
    @DisplayName("findById → retorna vacío para id inexistente")
    void findById_returnsEmpty_whenNotFound() {
        assertThat(votingRepository.findById(99999L)).isEmpty();
    }

    @Test
    @DisplayName("save → persiste nuevo voto con id generado")
    void save_persistsNewVoting() {
        Voting newVoting = new Voting(voter, project2, criterion1, 30);
        newVoting.setCategory(baseCategory);
        Voting saved = votingRepository.save(newVoting);

        assertThat(saved.getId()).isNotNull();
        em.clear();
        Long sId = saved.getId();
        Optional<Voting> reloaded = votingRepository.findById(Objects.requireNonNull(sId));
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getScore()).isEqualTo(30);
    }

    @Test
    @DisplayName("save → actualiza puntuación del voto existente (intervención manual)")
    void save_updatesScore_whenEdited() {
        voting1.setScore(40);
        votingRepository.save(voting1);
        em.flush(); em.clear();

        Voting updated = em.find(Voting.class, voting1.getId());
        assertThat(updated.getScore()).isEqualTo(40);
    }

    @Test
    @DisplayName("deleteById → elimina el voto de la BD")
    void deleteById_removesVoting() {
        Long id = voting1.getId();
        votingRepository.deleteById(Objects.requireNonNull(id));
        em.flush(); em.clear();

        assertThat(votingRepository.findById(Objects.requireNonNull(id))).isEmpty();
    }

    @Test
    @DisplayName("deleteById → no afecta a otros votos del mismo votante")
    void deleteById_doesNotAffectOtherVotings() {
        Long v1Id = voting1.getId();
        votingRepository.deleteById(Objects.requireNonNull(v1Id));
        em.flush(); em.clear();

        Long v2Id = voting2.getId();
        assertThat(votingRepository.findById(Objects.requireNonNull(v2Id))).isPresent();
    }

    @Test
    @DisplayName("count → cuenta correctamente los votos totales")
    void count_returnsCorrectTotal() {
        long count = votingRepository.count();
        assertThat(count).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("save → persiste el comentario junto al voto y lo devuelve al releer")
    void saveAndLoad_withComentario_persists() {
        Voting v = new Voting(voter, project2, criterion1, 15);
        v.setCategory(baseCategory);
        v.setComentario("La innovación es el punto más fuerte del proyecto");
        Voting saved = votingRepository.save(v);
        em.flush(); em.clear();

        Long sId = saved.getId();
        Optional<Voting> reloaded = votingRepository.findById(Objects.requireNonNull(sId));
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getComentario())
                .isEqualTo("La innovación es el punto más fuerte del proyecto");
    }

    @Test
    @DisplayName("findProjectScoresByCategoryId → suma weightedScore y usa score como fallback")
    void findProjectScoresByCategoryId_usesWeightedScoreAndFallbackScore() {
        Event event = new Event("Evento Ranking");
        em.persist(event);

        Category category = new Category("Coches", event);
        em.persist(category);

        Voting weightedVote = new Voting(voter, project1, criterion1, 8);
        weightedVote.setCategory(category);
        weightedVote.setWeightedScore(2.4);
        em.persist(weightedVote);

        Voting fallbackVote = new Voting(voter, project1, criterion2, 5);
        fallbackVote.setCategory(category);
        fallbackVote.setWeightedScore(null);
        em.persist(fallbackVote);

        Voting project2Vote = new Voting(voter, project2, criterion1, 7);
        project2Vote.setCategory(category);
        project2Vote.setWeightedScore(2.1);
        em.persist(project2Vote);

        em.flush();

        List<Object[]> rows = votingRepository.findProjectScoresByCategoryId(category.getId());

        assertThat(rows).hasSize(2);
        assertThat(((Long) rows.get(0)[0])).isEqualTo(project1.getId());
        assertThat(((Number) rows.get(0)[1]).doubleValue()).isEqualTo(7.4);
        assertThat(((Long) rows.get(1)[0])).isEqualTo(project2.getId());
        assertThat(((Number) rows.get(1)[1]).doubleValue()).isEqualTo(2.1);
    }
}
