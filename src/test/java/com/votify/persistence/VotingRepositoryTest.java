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

    private Voter      voter;
    private Competitor competitor1, competitor2;
    private Criterion  criterion1, criterion2;
    private Voting     voting1, voting2;

    @BeforeEach
    void setUp() {
        // Users base → Participants → Voter / Competitor (herencia JOINED)
        voter = new Voter("Jurado Test", "jurado_repo@test.com");
        em.persist(voter);

        competitor1 = new Competitor("Carlos Test", "carlos_repo@test.com");
        em.persist(competitor1);

        competitor2 = new Competitor("Ana Test", "ana_repo@test.com");
        em.persist(competitor2);

        criterion1 = new Criterion("Innovación");
        em.persist(criterion1);

        criterion2 = new Criterion("Presentación");
        em.persist(criterion2);

        voting1 = new Voting(voter, competitor1, criterion1, 25);
        em.persist(voting1);

        voting2 = new Voting(voter, competitor1, criterion2, 18);
        em.persist(voting2);

        em.flush();
    }

    // ── findAll ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll → retorna todos los votos persistidos")
    void findAll_returnsAllVotings() {
        List<Voting> all = votingRepository.findAll();
        assertThat(all).hasSizeGreaterThanOrEqualTo(2);
    }

    // ── findById ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById → retorna voto correcto con todas sus relaciones")
    void findById_returnsVotingWithRelations() {
        Long vId = voting1.getId();
        Optional<Voting> result = votingRepository.findById(Objects.requireNonNull(vId));

        assertThat(result).isPresent();
        Voting v = result.get();
        assertThat(v.getScore()).isEqualTo(25);
        assertThat(v.getVoter().getId()).isEqualTo(voter.getId());
        assertThat(v.getCompetitor().getId()).isEqualTo(competitor1.getId());
        assertThat(v.getCriterion().getId()).isEqualTo(criterion1.getId());
    }

    @Test
    @DisplayName("findById → retorna vacío para id inexistente")
    void findById_returnsEmpty_whenNotFound() {
        assertThat(votingRepository.findById(99999L)).isEmpty();
    }

    // ── save ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("save → persiste nuevo voto con id generado")
    void save_persistsNewVoting() {
        Voting newVoting = new Voting(voter, competitor2, criterion1, 30);
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

    // ── delete ─────────────────────────────────────────────────────────────

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

    // ── count ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("count → cuenta correctamente los votos totales")
    void count_returnsCorrectTotal() {
        long count = votingRepository.count();
        assertThat(count).isGreaterThanOrEqualTo(2);
    }
}
