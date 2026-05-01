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
import org.mockito.ArgumentCaptor;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
@DisplayName("VotingService — Tests unitarios")
class VotingServiceTest {

    @Mock private VotingRepository                  votingRepository;
    @Mock private UserRepository                    userRepository;
    @Mock private CriterionRepository               criterionRepository;
    @Mock private CategoryRepository                categoryRepository;
    @Mock private CategoryCriterionPointsRepository criterionPointsRepository;
    @Mock private ProjectRepository                 projectRepository;
    @Mock private CriterionService                  criterionService;

    @InjectMocks
    private VotingService votingService;

    private User      voter;
    private User      competitorUser;
    private Project   project;
    private Criterion criterion;
    private Voting    voting;

    @BeforeEach
    void setUp() {
        voter = new User("Jurado1", "jurado1@test.com", null);
        voter.setId(1L);

        competitorUser = new User("Competidor1", "comp1@test.com", null);
        competitorUser.setId(2L);

        Event event = new Event("Hackathon 2026");
        event.setId(50L);

        project = new Project("Pizza", "Proyecto Pizza", event);
        project.setId(10L);
        Set<User> competitors = new HashSet<>();
        competitors.add(competitorUser);
        project.setCompetitors(competitors);

        criterion = new Criterion("Innovación");
        criterion.setId(3L);

        voting = new Voting(voter, project, criterion, 25);
        voting.setId(100L);

        lenient().when(votingRepository.findExistingVote(anyLong(), anyLong(), anyLong(), (Long) any()))
                .thenReturn(Optional.empty());
    }

    // ── findAll ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll → retorna todos los votos")
    void findAll_returnsAllVotings() {
        Voting v2 = new Voting(voter, project, criterion, 18);
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
        assertThat(result.getProjectId()).isEqualTo(10L);
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
        when(userRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(criterionRepository.findById(3L)).thenReturn(Optional.of(criterion));
        when(votingRepository.save(any(Voting.class))).thenReturn(voting);

        VotingDto result = votingService.create(new VotingDto(null, 1L, 10L, 3L, 25));

        assertThat(result.getScore()).isEqualTo(25);
        assertThat(result.getProjectId()).isEqualTo(10L);
        verify(votingRepository, times(1)).save(any(Voting.class));
    }

    @Test
    @DisplayName("create → aplica ponderación por criterio de categoría para weightedScore")
    void create_appliesCriterionWeightingForWeightedScore() {
        Event event = new Event("Hackathon 2026");
        event.setId(50L);
        Category category = new Category("Jurado", event);
        category.setId(20L);

        CategoryCriterionPoints points = new CategoryCriterionPoints(category, criterion, 40);

        Voting persisted = new Voting(voter, project, criterion, 8);
        persisted.setId(300L);
        persisted.setCategory(category);
        persisted.setWeightedScore(3.2);
        persisted.setWeightingStrategy("criterionPoints");

        when(userRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(criterionRepository.findById(3L)).thenReturn(Optional.of(criterion));
        when(categoryRepository.findById(20L)).thenReturn(Optional.of(category));
        when(criterionPointsRepository.findByCategoryIdAndCriterionId(20L, 3L)).thenReturn(Optional.of(points));
        when(votingRepository.save(any(Voting.class))).thenReturn(persisted);

        VotingDto result = votingService.create(new VotingDto(null, 1L, 10L, 3L, 8, 20L));

        assertThat(result.getWeightedScore()).isEqualTo(3.2);
        assertThat(result.getWeightingStrategy()).isEqualTo("criterionPoints");
    }

    @Test
    @DisplayName("create → lanza excepción si el votante no existe")
    void create_throwsException_whenVoterNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> votingService.create(new VotingDto(null, 99L, 10L, 3L, 10)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Voter");
    }

    @Test
    @DisplayName("create → lanza excepción si el proyecto no existe")
    void create_throwsException_whenProjectNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> votingService.create(new VotingDto(null, 1L, 99L, 3L, 10)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Project");
    }

    @Test
    @DisplayName("create → lanza excepción si el proyecto no tiene competidores asignados")
    void create_throwsException_whenProjectHasNoCompetitors() {
        project.setCompetitors(new HashSet<>());
        when(userRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> votingService.create(new VotingDto(null, 1L, 10L, 3L, 10)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no tiene competidores");
    }

    @Test
    @DisplayName("create → lanza excepción si el votante es competidor del proyecto (auto-voto)")
    void create_throwsException_whenVoterIsCompetitorOfProject() {
        // voter (id=1) es competidor del proyecto
        Set<User> comps = new HashSet<>();
        comps.add(voter);
        project.setCompetitors(comps);

        when(userRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> votingService.create(new VotingDto(null, 1L, 10L, 3L, 10)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("propio proyecto");
    }

    @Test
    @DisplayName("create → lanza excepción si el criterio no existe")
    void create_throwsException_whenCriterionNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(criterionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> votingService.create(new VotingDto(null, 1L, 10L, 99L, 10)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Criterion");
    }

    // ── update ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update → modifica puntuación del voto existente")
    void update_changesScore() {
        when(votingRepository.findById(100L)).thenReturn(Optional.of(voting));
        when(userRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(criterionRepository.findById(3L)).thenReturn(Optional.of(criterion));

        Voting updatedVoting = new Voting(voter, project, criterion, 30);
        updatedVoting.setId(100L);
        when(votingRepository.save(any(Voting.class))).thenReturn(updatedVoting);

        VotingDto result = votingService.update(100L, new VotingDto(100L, 1L, 10L, 3L, 30));

        assertThat(result.getScore()).isEqualTo(30);
    }

    @Test
    @DisplayName("update → recalcula weightedScore con porcentaje de criterio")
    void update_recalculatesWeightedScoreUsingCriterionPercent() {
        Event event = new Event("Hackathon 2026");
        event.setId(50L);
        Category category = new Category("Jurado", event);
        category.setId(20L);

        voting.setCategory(category);
        voting.setScore(5);
        voting.setWeightedScore(0.0);

        CategoryCriterionPoints points = new CategoryCriterionPoints(category, criterion, 30);

        when(votingRepository.findById(100L)).thenReturn(Optional.of(voting));
        when(criterionPointsRepository.findByCategoryIdAndCriterionId(20L, 3L)).thenReturn(Optional.of(points));
        when(votingRepository.save(any(Voting.class))).thenAnswer(inv -> inv.getArgument(0));

        VotingDto result = votingService.update(100L, new VotingDto(100L, null, null, null, 8));

        assertThat(result.getScore()).isEqualTo(8);
        assertThat(result.getWeightedScore()).isEqualTo(2.4);
        assertThat(result.getWeightingStrategy()).isEqualTo("criterionPoints");

        ArgumentCaptor<Voting> captor = ArgumentCaptor.forClass(Voting.class);
        verify(votingRepository).save(captor.capture());
        assertThat(captor.getValue().getWeightedScore()).isEqualTo(2.4);
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
        when(userRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(criterionRepository.findById(3L)).thenReturn(Optional.of(criterion));

        Voting zeroVoting = new Voting(voter, project, criterion, 0);
        zeroVoting.setId(100L);
        when(votingRepository.save(any(Voting.class))).thenReturn(zeroVoting);

        VotingDto result = votingService.update(100L, new VotingDto(100L, 1L, 10L, 3L, 0));

        assertThat(result.getScore()).isEqualTo(0);
    }

    // ── Restricción POPULAR_VOTE (Req. 19 / Req. 23) ──────────────────────

    private Event testEvent;
    private Category popularCategory;

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
    @DisplayName("create POPULAR_VOTE → permite votar si no ha alcanzado el límite de proyectos")
    void create_popularVote_allowsVote_whenBelowMaxProjects() {
        setUpPopularVoteCategory(3, 10);

        when(userRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(criterionRepository.findById(3L)).thenReturn(Optional.of(criterion));
        when(categoryRepository.findById(20L)).thenReturn(Optional.of(popularCategory));

        when(votingRepository.countDistinctProjectsByVoterIdAndCategoryId(1L, 20L)).thenReturn(1L);
        when(votingRepository.findByVoterIdAndCategoryId(1L, 20L)).thenReturn(List.of());
        when(votingRepository.sumScoreByVoterIdAndCategoryId(1L, 20L)).thenReturn(3);

        Voting savedVoting = new Voting(voter, project, criterion, 2);
        savedVoting.setId(200L);
        savedVoting.setCategory(popularCategory);
        when(votingRepository.save(any(Voting.class))).thenReturn(savedVoting);

        VotingDto result = votingService.create(new VotingDto(null, 1L, 10L, 3L, 2, 20L));

        assertThat(result.getId()).isEqualTo(200L);
        verify(votingRepository, times(1)).save(any(Voting.class));
    }

    @Test
    @DisplayName("create POPULAR_VOTE → rechaza voto si supera el límite de 3 proyectos distintos")
    void create_popularVote_rejectsVote_whenMaxProjectsReached() {
        setUpPopularVoteCategory(3, null);

        // proyecto diferente al que ya votó (id=55L)
        Project anotherProject = new Project("Otro", "Otro proyecto", testEvent);
        anotherProject.setId(55L);
        Set<User> comps = new HashSet<>();
        comps.add(competitorUser);
        anotherProject.setCompetitors(comps);

        when(userRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(projectRepository.findById(55L)).thenReturn(Optional.of(anotherProject));
        when(criterionRepository.findById(3L)).thenReturn(Optional.of(criterion));
        when(categoryRepository.findById(20L)).thenReturn(Optional.of(popularCategory));

        when(votingRepository.countDistinctProjectsByVoterIdAndCategoryId(1L, 20L)).thenReturn(3L);
        // historial: ya votó al proyecto con id=10L
        Voting prevVoting = new Voting(voter, project, criterion, 2);
        prevVoting.setCategory(popularCategory);
        when(votingRepository.findByVoterIdAndCategoryId(1L, 20L)).thenReturn(List.of(prevVoting));

        assertThatThrownBy(() -> votingService.create(new VotingDto(null, 1L, 55L, 3L, 2, 20L)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("límite")
                .hasMessageContaining("3");
    }

    @Test
    @DisplayName("create POPULAR_VOTE → permite re-votar al mismo proyecto (no cuenta como nuevo)")
    void create_popularVote_allowsRevote_whenSameProject() {
        setUpPopularVoteCategory(3, 10);

        when(userRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(criterionRepository.findById(3L)).thenReturn(Optional.of(criterion));
        when(categoryRepository.findById(20L)).thenReturn(Optional.of(popularCategory));

        when(votingRepository.countDistinctProjectsByVoterIdAndCategoryId(1L, 20L)).thenReturn(3L);
        // historial: ya votó al mismo proyecto (id=10L) → no es proyecto nuevo
        Voting prevVoting = new Voting(voter, project, criterion, 1);
        prevVoting.setCategory(popularCategory);
        when(votingRepository.findByVoterIdAndCategoryId(1L, 20L)).thenReturn(List.of(prevVoting));
        when(votingRepository.sumScoreByVoterIdAndCategoryId(1L, 20L)).thenReturn(3);

        Voting savedVoting = new Voting(voter, project, criterion, 2);
        savedVoting.setId(201L);
        savedVoting.setCategory(popularCategory);
        when(votingRepository.save(any(Voting.class))).thenReturn(savedVoting);

        VotingDto result = votingService.create(new VotingDto(null, 1L, 10L, 3L, 2, 20L));
        assertThat(result.getId()).isEqualTo(201L);
    }

    @Test
    @DisplayName("create POPULAR_VOTE → rechaza voto si supera el totalPoints configurado")
    void create_popularVote_rejectsVote_whenExceedsTotalPoints() {
        setUpPopularVoteCategory(null, 10);

        when(userRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(criterionRepository.findById(3L)).thenReturn(Optional.of(criterion));
        when(categoryRepository.findById(20L)).thenReturn(Optional.of(popularCategory));

        when(votingRepository.sumScoreByVoterIdAndCategoryId(1L, 20L)).thenReturn(9);

        assertThatThrownBy(() -> votingService.create(new VotingDto(null, 1L, 10L, 3L, 5, 20L)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("puntos")
                .hasMessageContaining("10");
    }

    @Test
    @DisplayName("create POPULAR_VOTE → permite voto cuando la categoría no tiene límites configurados")
    void create_popularVote_allowsVote_whenNoLimitsConfigured() {
        setUpPopularVoteCategory(null, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(criterionRepository.findById(3L)).thenReturn(Optional.of(criterion));
        when(categoryRepository.findById(20L)).thenReturn(Optional.of(popularCategory));

        Voting savedVoting = new Voting(voter, project, criterion, 5);
        savedVoting.setId(202L);
        savedVoting.setCategory(popularCategory);
        when(votingRepository.save(any(Voting.class))).thenReturn(savedVoting);

        VotingDto result = votingService.create(new VotingDto(null, 1L, 10L, 3L, 5, 20L));
        assertThat(result.getId()).isEqualTo(202L);
    }

    // ── PA-1314 — Guard Clause: voto duplicado ─────────────────────────────

    @Test
    @DisplayName("create → lanza excepción si ya existe un voto para esa combinación (PA-1314)")
    void create_throwsException_whenDuplicateVote() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(criterionRepository.findById(3L)).thenReturn(Optional.of(criterion));
        // override lenient stub: ya existe voto
        when(votingRepository.findExistingVote(1L, 10L, 3L, null))
                .thenReturn(Optional.of(voting));

        assertThatThrownBy(() -> votingService.create(new VotingDto(null, 1L, 10L, 3L, 10)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Ya has votado");
    }

    @Test
    @DisplayName("create → no persiste nada si el voto ya existe (PA-1314)")
    void create_neverSaves_whenDuplicateVote() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(criterionRepository.findById(3L)).thenReturn(Optional.of(criterion));
        when(votingRepository.findExistingVote(1L, 10L, 3L, null))
                .thenReturn(Optional.of(voting));

        assertThatThrownBy(() -> votingService.create(new VotingDto(null, 1L, 10L, 3L, 10)))
                .isInstanceOf(RuntimeException.class);
        verify(votingRepository, never()).save(any(Voting.class));
    }

    // ── PA-1315 — No persiste si auto-voto ────────────────────────────────

    @Test
    @DisplayName("create → no persiste nada si el votante es competidor del proyecto (PA-1315)")
    void create_neverSaves_whenVoterIsCompetitorOfProject() {
        Set<User> comps = new HashSet<>();
        comps.add(voter);
        project.setCompetitors(comps);

        when(userRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> votingService.create(new VotingDto(null, 1L, 10L, 3L, 10)))
                .isInstanceOf(RuntimeException.class);
        verify(votingRepository, never()).save(any(Voting.class));
    }

    // ── PA-1313 — Periodo de votación ──────────────────────────────────────

    @Test
    @DisplayName("create → lanza excepción si el periodo de votación ya ha cerrado (PA-1313)")
    void create_throwsException_whenPeriodClosed() {
        Event ev = new Event("Hackathon");
        ev.setId(99L);
        Category closedCat = new Category("Cerrada", ev);
        closedCat.setId(30L);
        closedCat.setVotingType(VotingType.POPULAR_VOTE);
        closedCat.setTimeInitial(new java.util.Date(System.currentTimeMillis() - 7_200_000L));
        closedCat.setTimeFinal(new java.util.Date(System.currentTimeMillis() - 3_600_000L));

        when(userRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(criterionRepository.findById(3L)).thenReturn(Optional.of(criterion));
        when(categoryRepository.findById(30L)).thenReturn(Optional.of(closedCat));

        assertThatThrownBy(() -> votingService.create(new VotingDto(null, 1L, 10L, 3L, 5, 30L)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("periodo");
    }

    @Test
    @DisplayName("create → lanza excepción si el periodo de votación todavía no ha empezado (PA-1313)")
    void create_throwsException_whenPeriodNotStartedYet() {
        Event ev = new Event("Hackathon");
        ev.setId(99L);
        Category futureCat = new Category("Futura", ev);
        futureCat.setId(31L);
        futureCat.setVotingType(VotingType.POPULAR_VOTE);
        futureCat.setTimeInitial(new java.util.Date(System.currentTimeMillis() + 3_600_000L));
        futureCat.setTimeFinal(new java.util.Date(System.currentTimeMillis() + 7_200_000L));

        when(userRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(criterionRepository.findById(3L)).thenReturn(Optional.of(criterion));
        when(categoryRepository.findById(31L)).thenReturn(Optional.of(futureCat));

        assertThatThrownBy(() -> votingService.create(new VotingDto(null, 1L, 10L, 3L, 5, 31L)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("periodo");
    }

    // ── PA-1316 — Intervención manual ──────────────────────────────────────

    @Test
    @DisplayName("update → marca manuallyModified=true en intervención manual (PA-1316)")
    void update_setsManuallyModifiedTrue_onManualIntervention() {
        when(votingRepository.findById(100L)).thenReturn(Optional.of(voting));
        when(userRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(criterionRepository.findById(3L)).thenReturn(Optional.of(criterion));

        Voting manualVoting = new Voting(voter, project, criterion, 40);
        manualVoting.setId(100L);
        manualVoting.setManuallyModified(true);
        when(votingRepository.save(any(Voting.class))).thenReturn(manualVoting);

        VotingDto dto = new VotingDto(100L, 1L, 10L, 3L, 40);
        dto.setManuallyModified(true);
        VotingDto result = votingService.update(100L, dto);

        assertThat(result.getManuallyModified()).isTrue();
    }

    @Test
    @DisplayName("update → mantiene manuallyModified=false si no es intervención manual (PA-1316)")
    void update_keepsManuallyModifiedFalse_whenNotManual() {
        voting.setManuallyModified(false);
        when(votingRepository.findById(100L)).thenReturn(Optional.of(voting));
        when(userRepository.findById(1L)).thenReturn(Optional.of(voter));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(criterionRepository.findById(3L)).thenReturn(Optional.of(criterion));

        Voting normalVoting = new Voting(voter, project, criterion, 30);
        normalVoting.setId(100L);
        normalVoting.setManuallyModified(false);
        when(votingRepository.save(any(Voting.class))).thenReturn(normalVoting);

        VotingDto dto = new VotingDto(100L, 1L, 10L, 3L, 30);
        dto.setManuallyModified(false);
        VotingDto result = votingService.update(100L, dto);

        assertThat(result.getManuallyModified()).isFalse();
    }
}
