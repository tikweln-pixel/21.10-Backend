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

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
@DisplayName("VotingService — Tests unitarios")
class VotingServiceTest {

    @Mock
    private VotingRepository votingRepository;
    @Mock
    private VoterRepository voterRepository;
    @Mock
    private CompetitorRepository competitorRepository;
    @Mock
    private CriterionRepository criterionRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private com.votify.persistence.CategoryCriterionPointsRepository criterionPointsRepository;

    @InjectMocks
    private VotingService votingService;

    private Voter voter;
    private Competitor competitor;
    private Criterion criterion;
    private Voting voting;

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
        // lenient: este stub no lo usan todos los tests (findAll, findById, delete,
        // etc.)
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
    private Event testEvent;
    private Category popularCategory;
    private Category juryCategory;

    private void setUpJuryCategory(Date timeInitial, Date timeFinal) {
        testEvent = new Event("Hackathon");
        testEvent.setId(99L);

        juryCategory = new Category("Jurado Técnico", testEvent);
        juryCategory.setId(30L);
        juryCategory.setVotingType(VotingType.JURY_EXPERT);
        juryCategory.setTimeInitial(timeInitial);
        juryCategory.setTimeFinal(timeFinal);
    }

    private Date past() { return new Date(System.currentTimeMillis() - 86_400_000L); }
    private Date future() { return new Date(System.currentTimeMillis() + 86_400_000L); }

    private void setUpPopularVoteCategory(Integer maxVotesPerVoter, Integer totalPoints) {
        testEvent = new Event("Hackathon");
        testEvent.setId(99L);

        popularCategory = new Category("Voto Popular", testEvent);
        popularCategory.setId(20L);
        popularCategory.setVotingType(VotingType.POPULAR_VOTE);
        popularCategory.setMaxVotesPerVoter(maxVotesPerVoter);
        popularCategory.setTotalPoints(totalPoints);
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

        assertThatThrownBy(() -> votingService.create(new VotingDto(null, 1L, 5L, 3L, 2, 20L)))
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

        assertThatThrownBy(() -> votingService.create(new VotingDto(null, 1L, 2L, 3L, 5, 20L)))
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

    // ── Comentarios por criterio (UT Votar con Comentarios — Sprint 1) ────

    @Test
    @DisplayName("create → nuevo voto con comentario persiste el comentario recortado")
    void create_newVote_withComment_persistsComment() {
        when(voterRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(competitorRepository.findById(2L)).thenReturn(Optional.of(competitor));
        when(criterionRepository.findById(3L)).thenReturn(Optional.of(criterion));
        when(votingRepository.save(any(Voting.class))).thenAnswer(inv -> inv.getArgument(0));

        VotingDto dto = new VotingDto(null, 1L, 2L, 3L, 8);
        dto.setComentario("  me encanta la innovación  ");
        VotingDto result = votingService.create(dto);

        ArgumentCaptor<Voting> captor = ArgumentCaptor.forClass(Voting.class);
        verify(votingRepository).save(captor.capture());
        assertThat(captor.getValue().getComentario()).isEqualTo("me encanta la innovación");
        assertThat(result.getComentario()).isEqualTo("me encanta la innovación");
    }

    @Test
    @DisplayName("create → voto existente, comentario nuevo sobrescribe el anterior")
    void create_existingVote_overwritesComment() {
        Voting existing = new Voting(voter, competitor, criterion, 5);
        existing.setId(500L);
        existing.setComentario("comentario viejo");

        when(voterRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(competitorRepository.findById(2L)).thenReturn(Optional.of(competitor));
        when(criterionRepository.findById(3L)).thenReturn(Optional.of(criterion));
        when(votingRepository.findExistingVote(1L, 2L, 3L, null)).thenReturn(Optional.of(existing));
        when(votingRepository.save(any(Voting.class))).thenAnswer(inv -> inv.getArgument(0));

        VotingDto dto = new VotingDto(null, 1L, 2L, 3L, 2);
        dto.setComentario("comentario nuevo");
        votingService.create(dto);

        assertThat(existing.getComentario()).isEqualTo("comentario nuevo");
    }

    @Test
    @DisplayName("create → comentario vacío / blanco se guarda como NULL")
    void create_emptyComment_storesNull() {
        when(voterRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(competitorRepository.findById(2L)).thenReturn(Optional.of(competitor));
        when(criterionRepository.findById(3L)).thenReturn(Optional.of(criterion));
        when(votingRepository.save(any(Voting.class))).thenAnswer(inv -> inv.getArgument(0));

        VotingDto dto = new VotingDto(null, 1L, 2L, 3L, 4);
        dto.setComentario("   ");
        votingService.create(dto);

        ArgumentCaptor<Voting> captor = ArgumentCaptor.forClass(Voting.class);
        verify(votingRepository).save(captor.capture());
        assertThat(captor.getValue().getComentario()).isNull();
    }

    @Test
    @DisplayName("create → comentario > 500 caracteres lanza excepción")
    void create_overlongComment_throws() {
        when(voterRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(competitorRepository.findById(2L)).thenReturn(Optional.of(competitor));
        when(criterionRepository.findById(3L)).thenReturn(Optional.of(criterion));

        String overlong = "a".repeat(501);
        VotingDto dto = new VotingDto(null, 1L, 2L, 3L, 4);
        dto.setComentario(overlong);

        assertThatThrownBy(() -> votingService.create(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("500");
    }

    // ── Control de Votos — Pruebas de Aceptación (Sprint 2) ───────────────

    @Test
    @DisplayName("PA-1314-1 → re-voto JURY_EXPERT actualiza score sin crear nueva fila")
    void create_juryExpert_revote_updatesExistingScore() {
        setUpJuryCategory(past(), future());

        when(voterRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(competitorRepository.findById(2L)).thenReturn(Optional.of(competitor));
        when(criterionRepository.findById(3L)).thenReturn(Optional.of(criterion));
        when(categoryRepository.findById(30L)).thenReturn(Optional.of(juryCategory));

        Voting existing = new Voting(voter, competitor, criterion, 5);
        existing.setId(500L);
        existing.setCategory(juryCategory);
        when(votingRepository.findExistingVote(1L, 2L, 3L, 30L)).thenReturn(Optional.of(existing));

        CategoryCriterionPoints pts = new CategoryCriterionPoints(juryCategory, criterion, 50);
        when(criterionPointsRepository.findByCategoryIdAndCriterionId(30L, 3L)).thenReturn(Optional.of(pts));
        when(votingRepository.save(any(Voting.class))).thenAnswer(inv -> inv.getArgument(0));

        votingService.create(new VotingDto(null, 1L, 2L, 3L, 8, 30L));

        // score reemplazado (JURY_EXPERT), no acumulado; save llamado una sola vez
        assertThat(existing.getScore()).isEqualTo(8);
        verify(votingRepository, times(1)).save(any(Voting.class));
    }

    @Test
    @DisplayName("Control de Votos → rechaza voto cuando el periodo de votación ha cerrado")
    void create_rejectsVote_whenPeriodClosed() {
        setUpJuryCategory(past(), past()); // timeFinal ya pasó

        when(voterRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(competitorRepository.findById(2L)).thenReturn(Optional.of(competitor));
        when(criterionRepository.findById(3L)).thenReturn(Optional.of(criterion));
        when(categoryRepository.findById(30L)).thenReturn(Optional.of(juryCategory));

        assertThatThrownBy(() -> votingService.create(new VotingDto(null, 1L, 2L, 3L, 5, 30L)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("periodo");
    }

    @Test
    @DisplayName("Control de Votos → rechaza voto cuando el periodo aún no ha comenzado")
    void create_rejectsVote_whenPeriodNotStarted() {
        setUpJuryCategory(future(), future()); // timeInitial en el futuro

        when(voterRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(competitorRepository.findById(2L)).thenReturn(Optional.of(competitor));
        when(criterionRepository.findById(3L)).thenReturn(Optional.of(criterion));
        when(categoryRepository.findById(30L)).thenReturn(Optional.of(juryCategory));

        assertThatThrownBy(() -> votingService.create(new VotingDto(null, 1L, 2L, 3L, 5, 30L)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("periodo");
    }

    @Test
    @DisplayName("Control de Votos → permite voto dentro del periodo activo")
    void create_allowsVote_whenPeriodActive() {
        setUpJuryCategory(past(), future()); // periodo abierto

        when(voterRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(competitorRepository.findById(2L)).thenReturn(Optional.of(competitor));
        when(criterionRepository.findById(3L)).thenReturn(Optional.of(criterion));
        when(categoryRepository.findById(30L)).thenReturn(Optional.of(juryCategory));

        CategoryCriterionPoints pts = new CategoryCriterionPoints(juryCategory, criterion, 50);
        when(criterionPointsRepository.findByCategoryIdAndCriterionId(30L, 3L)).thenReturn(Optional.of(pts));

        Voting saved = new Voting(voter, competitor, criterion, 7);
        saved.setId(600L);
        saved.setCategory(juryCategory);
        when(votingRepository.save(any(Voting.class))).thenReturn(saved);

        VotingDto result = votingService.create(new VotingDto(null, 1L, 2L, 3L, 7, 30L));

        assertThat(result.getId()).isEqualTo(600L);
        verify(votingRepository, times(1)).save(any(Voting.class));
    }

    @Test
    @DisplayName("PA-1317-1 → bloqueo en tiempo real: rechaza voto con periodo recién cerrado")
    void create_rejectsVote_whenPeriodJustClosed() {
        // Simula que el periodo expiró milisegundos antes del intento de voto
        Date justPast = new Date(System.currentTimeMillis() - 1L);
        setUpJuryCategory(past(), justPast);

        when(voterRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(competitorRepository.findById(2L)).thenReturn(Optional.of(competitor));
        when(criterionRepository.findById(3L)).thenReturn(Optional.of(criterion));
        when(categoryRepository.findById(30L)).thenReturn(Optional.of(juryCategory));

        assertThatThrownBy(() -> votingService.create(new VotingDto(null, 1L, 2L, 3L, 5, 30L)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("periodo");
    }

    @Test
    @DisplayName("PA-1316-1 → intervención manual por supervisor marca manuallyModified=true")
    void update_supervisorIntervention_setsManuallyModified() {
        voting.setManuallyModified(false);
        when(votingRepository.findById(100L)).thenReturn(Optional.of(voting));
        when(voterRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(competitorRepository.findById(2L)).thenReturn(Optional.of(competitor));
        when(criterionRepository.findById(3L)).thenReturn(Optional.of(criterion));
        when(votingRepository.save(any(Voting.class))).thenAnswer(inv -> inv.getArgument(0));

        VotingDto dto = new VotingDto(100L, 1L, 2L, 3L, 40);
        dto.setManuallyModified(true);
        votingService.update(100L, dto);

        ArgumentCaptor<Voting> captor = ArgumentCaptor.forClass(Voting.class);
        verify(votingRepository).save(captor.capture());
        assertThat(captor.getValue().getManuallyModified()).isTrue();
        assertThat(captor.getValue().getScore()).isEqualTo(40);
    }

    @Test
    @DisplayName("Control de Votos → competidor no puede votar su propio proyecto")
    void create_rejectsVote_whenSelfVote() {
        Voter selfVoter = new Voter("AutoVoter", "auto@test.com");
        selfVoter.setId(7L);
        Competitor selfCompetitor = new Competitor("AutoVoter", "auto@test.com");
        selfCompetitor.setId(7L); // mismo userId → auto-voto

        when(voterRepository.findById(7L)).thenReturn(Optional.of(selfVoter));
        when(competitorRepository.findById(7L)).thenReturn(Optional.of(selfCompetitor));

        assertThatThrownBy(() -> votingService.create(new VotingDto(null, 7L, 7L, 3L, 5)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("propio");
    }

    @Test
    @DisplayName("update → sin comentario en DTO no borra el comentario existente")
    void update_nullComment_preservesExisting() {
        voting.setComentario("comentario previo");
        when(votingRepository.findById(100L)).thenReturn(Optional.of(voting));
        when(voterRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(competitorRepository.findById(2L)).thenReturn(Optional.of(competitor));
        when(criterionRepository.findById(3L)).thenReturn(Optional.of(criterion));
        when(votingRepository.save(any(Voting.class))).thenAnswer(inv -> inv.getArgument(0));

        VotingDto dto = new VotingDto(100L, 1L, 2L, 3L, 30);
        // dto.comentario queda null explícitamente
        votingService.update(100L, dto);

        assertThat(voting.getComentario()).isEqualTo("comentario previo");
    }
}
