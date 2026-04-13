package com.votify.service;

import com.votify.dto.VotingDto;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
@DisplayName("VotingService — Tests unitarios")
class VotingServiceTest {

    @Mock private VotingRepository     votingRepository;
    @Mock private VoterRepository      voterRepository;
    @Mock private CompetitorRepository competitorRepository;
    @Mock private CriterionRepository  criterionRepository;
    @Mock private CategoryRepository   categoryRepository;
    @Mock private com.votify.persistence.CategoryCriterionPointsRepository criterionPointsRepository;

    @InjectMocks
    private VotingService votingService;

    private Voter      voter;
    private Competitor competitor;
    private Criterion  criterion;
    private Voting     voting;

    @BeforeEach
    void setUp() {
        voter = new Voter("Jurado1", "jurado1@test.com");
        voter.setId(1L);

        competitor = new Competitor("Carlos", "carlos@test.com");
        competitor.setId(2L);

        criterion = new Criterion("Innovación");
        criterion.setId(3L);

        voting = new Voting(voter, competitor, criterion, 25);
        voting.setId(100L);

        // Por defecto, no existe un voto previo entre las entidades usadas en los tests
        // lenient: este stub no lo usan todos los tests (findAll, findById, delete, etc.)
        lenient().when(votingRepository.findExistingVote(anyLong(), anyLong(), anyLong(), (Long) any()))
                .thenReturn(java.util.Optional.empty());
    }

    // ── findAll ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll → retorna todos los votos")
    void findAll_returnsAllVotings() {
        Voting v2 = new Voting(voter, competitor, criterion, 18);
        v2.setId(101L);
        when(votingRepository.findAll()).thenReturn(List.of(voting, v2));

        List<VotingDto> result = votingService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(VotingDto::getScore).containsExactlyInAnyOrder(25, 18);
    }

    // ── findById ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById → retorna DTO correcto cuando existe")
    void findById_returnsDto_whenFound() {
        when(votingRepository.findById(100L)).thenReturn(Optional.of(voting));

        VotingDto result = votingService.findById(100L);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getScore()).isEqualTo(25);
        assertThat(result.getVoterId()).isEqualTo(1L);
        assertThat(result.getCompetitorId()).isEqualTo(2L);
        assertThat(result.getCriterionId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("findById → lanza excepción cuando no existe")
    void findById_throwsException_whenNotFound() {
        when(votingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> votingService.findById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("999");
    }

    // ── create ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create → crea y guarda voto con entidades correctas")
    void create_savesVotingWithCorrectEntities() {
        when(voterRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(competitorRepository.findById(2L)).thenReturn(Optional.of(competitor));
        when(criterionRepository.findById(3L)).thenReturn(Optional.of(criterion));
        when(votingRepository.save(any(Voting.class))).thenReturn(voting);

        VotingDto dto = new VotingDto(null, 1L, 2L, 3L, 25);
        VotingDto result = votingService.create(dto);

        assertThat(result.getScore()).isEqualTo(25);
        verify(votingRepository, times(1)).save(any(Voting.class));
    }

    @Test
    @DisplayName("create → lanza excepción si el votante no existe")
    void create_throwsException_whenVoterNotFound() {
        when(voterRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> votingService.create(new VotingDto(null, 99L, 2L, 3L, 10)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Voter");
    }

    @Test
    @DisplayName("create → lanza excepción si el competidor no existe")
    void create_throwsException_whenCompetitorNotFound() {
        when(voterRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(competitorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> votingService.create(new VotingDto(null, 1L, 99L, 3L, 10)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Competitor");
    }

    @Test
    @DisplayName("create → lanza excepción si el criterio no existe")
    void create_throwsException_whenCriterionNotFound() {
        when(voterRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(competitorRepository.findById(2L)).thenReturn(Optional.of(competitor));
        when(criterionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> votingService.create(new VotingDto(null, 1L, 2L, 99L, 10)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Criterion");
    }

    // ── update ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update → modifica puntuación del voto existente")
    void update_changesScore() {
        when(votingRepository.findById(100L)).thenReturn(Optional.of(voting));
        when(voterRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(competitorRepository.findById(2L)).thenReturn(Optional.of(competitor));
        when(criterionRepository.findById(3L)).thenReturn(Optional.of(criterion));

        Voting updatedVoting = new Voting(voter, competitor, criterion, 30);
        updatedVoting.setId(100L);
        when(votingRepository.save(any(Voting.class))).thenReturn(updatedVoting);

        VotingDto result = votingService.update(100L, new VotingDto(100L, 1L, 2L, 3L, 30));

        assertThat(result.getScore()).isEqualTo(30);
    }

    // ── delete ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete → llama a deleteById con el id correcto")
    void delete_callsDeleteById() {
        doNothing().when(votingRepository).deleteById(100L);

        votingService.delete(100L);

        verify(votingRepository, times(1)).deleteById(100L);
    }

    // ── Intervención manual ────────────────────────────────────────────────

    @Test
    @DisplayName("update (intervención) → permite cambiar score a 0")
    void update_allowsScoreZero() {
        when(votingRepository.findById(100L)).thenReturn(Optional.of(voting));
        when(voterRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(competitorRepository.findById(2L)).thenReturn(Optional.of(competitor));
        when(criterionRepository.findById(3L)).thenReturn(Optional.of(criterion));

        Voting zeroVoting = new Voting(voter, competitor, criterion, 0);
        zeroVoting.setId(100L);
        when(votingRepository.save(any(Voting.class))).thenReturn(zeroVoting);

        VotingDto result = votingService.update(100L, new VotingDto(100L, 1L, 2L, 3L, 0));

        assertThat(result.getScore()).isEqualTo(0);
    }

    // ── Restricción POPULAR_VOTE (Req. 19 / Req. 23) ──────────────────────

    // Helpers privados del test
    private Event   testEvent;
    private Category popularCategory;

    private void setUpPopularVoteCategory(Integer maxVotesPerVoter, Integer totalPoints) {
        testEvent = new Event("Hackathon");
        testEvent.setId(99L);

        popularCategory = new Category("Voto Popular", testEvent);
        popularCategory.setId(20L);
        popularCategory.changeVotingType(VotingType.POPULAR_VOTE);
        popularCategory.limitVotesPerVoter(maxVotesPerVoter);
        popularCategory.configureTotalPoints(totalPoints);
    }

    @Test
    @DisplayName("create POPULAR_VOTE → permite votar si no ha alcanzado el límite de competidores")
    void create_popularVote_allowsVote_whenBelowMaxCompetitors() {
        setUpPopularVoteCategory(3, 10);

        when(voterRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(competitorRepository.findById(2L)).thenReturn(Optional.of(competitor));
        when(criterionRepository.findById(3L)).thenReturn(Optional.of(criterion));
        when(categoryRepository.findById(20L)).thenReturn(Optional.of(popularCategory));

        // Solo ha votado a 1 competidor distinto, límite es 3 → OK
        when(votingRepository.countDistinctCompetitorsByVoterIdAndCategoryId(1L, 20L)).thenReturn(1L);
        when(votingRepository.findByVoterIdAndCategoryId(1L, 20L)).thenReturn(List.of());
        when(votingRepository.sumScoreByVoterIdAndCategoryId(1L, 20L)).thenReturn(3);

        Voting savedVoting = new Voting(voter, competitor, criterion, 2);
        savedVoting.setId(200L);
        savedVoting.setCategory(popularCategory);
        when(votingRepository.save(any(Voting.class))).thenReturn(savedVoting);

        VotingDto result = votingService.create(new VotingDto(null, 1L, 2L, 3L, 2, 20L));

        assertThat(result.getId()).isEqualTo(200L);
        verify(votingRepository, times(1)).save(any(Voting.class));
    }

    @Test
    @DisplayName("create POPULAR_VOTE → rechaza voto si supera el límite de 3 competidores de 5")
    void create_popularVote_rejectsVote_whenMaxCompetitorsReached() {
        setUpPopularVoteCategory(3, null); // límite 3, sin restricción de puntos

        Competitor competitor2 = new Competitor("Ana", "ana@test.com");
        competitor2.setId(5L);

        when(voterRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(competitorRepository.findById(5L)).thenReturn(Optional.of(competitor2));
        when(criterionRepository.findById(3L)).thenReturn(Optional.of(criterion));
        when(categoryRepository.findById(20L)).thenReturn(Optional.of(popularCategory));

        // Ya votó a 3 competidores distintos, intenta votar a un cuarto → rechazar
        when(votingRepository.countDistinctCompetitorsByVoterIdAndCategoryId(1L, 20L)).thenReturn(3L);

        // competitor2 (id=5) no está en los votos previos → es nuevo
        Voting prevVoting = new Voting(voter, competitor, criterion, 2);
        prevVoting.setCategory(popularCategory);
        when(votingRepository.findByVoterIdAndCategoryId(1L, 20L)).thenReturn(List.of(prevVoting));

        assertThatThrownBy(() ->
                votingService.create(new VotingDto(null, 1L, 5L, 3L, 2, 20L)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("límite")
                .hasMessageContaining("3");
    }

    @Test
    @DisplayName("create POPULAR_VOTE → permite votar al mismo competidor ya votado (no cuenta como nuevo)")
    void create_popularVote_allowsRevote_whenSameCompetitor() {
        setUpPopularVoteCategory(3, 10);

        when(voterRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(competitorRepository.findById(2L)).thenReturn(Optional.of(competitor));
        when(criterionRepository.findById(3L)).thenReturn(Optional.of(criterion));
        when(categoryRepository.findById(20L)).thenReturn(Optional.of(popularCategory));

        // Ya votó a 3, pero el competidor 2 ya está en los votos previos → no es nuevo
        when(votingRepository.countDistinctCompetitorsByVoterIdAndCategoryId(1L, 20L)).thenReturn(3L);
        Voting prevVoting = new Voting(voter, competitor, criterion, 1); // competitor.id = 2
        prevVoting.setCategory(popularCategory);
        when(votingRepository.findByVoterIdAndCategoryId(1L, 20L)).thenReturn(List.of(prevVoting));
        when(votingRepository.sumScoreByVoterIdAndCategoryId(1L, 20L)).thenReturn(3);

        Voting savedVoting = new Voting(voter, competitor, criterion, 2);
        savedVoting.setId(201L);
        savedVoting.setCategory(popularCategory);
        when(votingRepository.save(any(Voting.class))).thenReturn(savedVoting);

        // Votar de nuevo al mismo competidor no debe lanzar excepción
        VotingDto result = votingService.create(new VotingDto(null, 1L, 2L, 3L, 2, 20L));
        assertThat(result.getId()).isEqualTo(201L);
    }

    @Test
    @DisplayName("create POPULAR_VOTE → rechaza voto si supera el totalPoints configurado")
    void create_popularVote_rejectsVote_whenExceedsTotalPoints() {
        setUpPopularVoteCategory(null, 10); // sin límite de competidores, totalPoints = 10

        when(voterRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(competitorRepository.findById(2L)).thenReturn(Optional.of(competitor));
        when(criterionRepository.findById(3L)).thenReturn(Optional.of(criterion));
        when(categoryRepository.findById(20L)).thenReturn(Optional.of(popularCategory));

        // Ya usó 9 puntos, intenta asignar 5 más → 9+5=14 > 10 → rechazar
        when(votingRepository.sumScoreByVoterIdAndCategoryId(1L, 20L)).thenReturn(9);

        assertThatThrownBy(() ->
                votingService.create(new VotingDto(null, 1L, 2L, 3L, 5, 20L)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("puntos")
                .hasMessageContaining("10");
    }

    @Test
    @DisplayName("create POPULAR_VOTE → permite voto cuando la categoría no tiene límites configurados")
    void create_popularVote_allowsVote_whenNoLimitsConfigured() {
        setUpPopularVoteCategory(null, null); // sin ninguna restricción

        when(voterRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(competitorRepository.findById(2L)).thenReturn(Optional.of(competitor));
        when(criterionRepository.findById(3L)).thenReturn(Optional.of(criterion));
        when(categoryRepository.findById(20L)).thenReturn(Optional.of(popularCategory));

        Voting savedVoting = new Voting(voter, competitor, criterion, 5);
        savedVoting.setId(202L);
        savedVoting.setCategory(popularCategory);
        when(votingRepository.save(any(Voting.class))).thenReturn(savedVoting);

        VotingDto result = votingService.create(new VotingDto(null, 1L, 2L, 3L, 5, 20L));
        assertThat(result.getId()).isEqualTo(202L);
    }
}
