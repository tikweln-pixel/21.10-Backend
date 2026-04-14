package com.votify.service;

import com.votify.dto.CategoryCriterionPointsDto;
import com.votify.dto.CategoryDto;
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
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService — Tests unitarios")
class CategoryServiceTest {

    @Mock private CategoryRepository               categoryRepository;
    @Mock private EventRepository                  eventRepository;
    @Mock private CriterionRepository              criterionRepository;
    @Mock private CategoryCriterionPointsRepository criterionPointsRepository;
    @Mock private VotingRepository                 votingRepository;
    @Mock private EventParticipationRepository     eventParticipationRepository;
    @Mock private EvaluacionRepository             evaluacionRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Event  event;
    private Category category;
    private Criterion criterion;

    @BeforeEach
    void setUp() {
        event = new Event("Hackathon 2026");
        event.setId(1L);
        event.setTimeInitial(new Date(0));
        event.setTimeFinal(new Date(Long.MAX_VALUE));

        category = new Category("Jurado Experto", event);
        category.setId(10L);

        criterion = new Criterion("Innovación");
        criterion.setId(1L);
    }

    // ── findAll ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll → retorna todas las categorías")
    void findAll_returnsAllCategories() {
        Category cat2 = new Category("Voto Popular", event);
        cat2.setId(11L);
        when(categoryRepository.findAll()).thenReturn(List.of(category, cat2));

        List<CategoryDto> result = categoryService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(CategoryDto::getName)
                .containsExactlyInAnyOrder("Jurado Experto", "Voto Popular");
    }

    // ── findByEventId ──────────────────────────────────────────────────────

    @Test
    @DisplayName("findByEventId → retorna solo las categorías del evento")
    void findByEventId_returnsOnlyCategoriesOfEvent() {
        when(categoryRepository.findByEventId(1L)).thenReturn(List.of(category));

        List<CategoryDto> result = categoryService.findByEventId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEventId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("findByEventId → retorna vacío para evento sin categorías")
    void findByEventId_returnsEmpty_whenNoCategoriesForEvent() {
        when(categoryRepository.findByEventId(99L)).thenReturn(List.of());
        assertThat(categoryService.findByEventId(99L)).isEmpty();
    }

    // ── findById ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById → retorna DTO correcto")
    void findById_returnsDto() {
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        CategoryDto result = categoryService.findById(10L);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getName()).isEqualTo("Jurado Experto");
    }

    @Test
    @DisplayName("findById → lanza excepción si no existe")
    void findById_throwsException_whenNotFound() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.findById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("999");
    }

    // ── createForEvent ─────────────────────────────────────────────────────

    @Test
    @DisplayName("createForEvent → crea categoría asociada al evento")
    void createForEvent_createsCategoryLinkedToEvent() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        Category saved = new Category("Nueva Cat", event);
        saved.setId(20L);
        when(categoryRepository.save(any(Category.class))).thenReturn(Objects.requireNonNull(saved));

        CategoryDto result = categoryService.createForEvent(1L, "Nueva Cat");

        assertThat(result.getId()).isEqualTo(20L);
        assertThat(result.getName()).isEqualTo("Nueva Cat");
        assertThat(result.getEventId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("createForEvent → lanza excepción si el evento no existe")
    void createForEvent_throwsException_whenEventNotFound() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.createForEvent(99L, "Test"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    // ── setVotingType ──────────────────────────────────────────────────────

    @Test
    @DisplayName("setVotingType → asigna JURY_EXPERT correctamente")
    void setVotingType_setsJuryExpert() {
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        category.changeVotingType(VotingType.JURY_EXPERT);
        when(categoryRepository.save(any(Category.class))).thenReturn(Objects.requireNonNull(category));

        CategoryDto result = categoryService.setVotingType(10L, VotingType.JURY_EXPERT);

        assertThat(result.getVotingType()).isEqualTo(VotingType.JURY_EXPERT);
    }

    @Test
    @DisplayName("setVotingType → asigna POPULAR_VOTE correctamente")
    void setVotingType_setsPopularVote() {
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        category.changeVotingType(VotingType.POPULAR_VOTE);
        when(categoryRepository.save(any(Category.class))).thenReturn(Objects.requireNonNull(category));

        CategoryDto result = categoryService.setVotingType(10L, VotingType.POPULAR_VOTE);

        assertThat(result.getVotingType()).isEqualTo(VotingType.POPULAR_VOTE);
    }

    // ── setCriterionPointsBulk ─────────────────────────────────────────────

    @Test
    @DisplayName("setCriterionPointsBulk → guarda puntos correctamente")
    void setCriterionPointsBulk_savesPointsForEachCriterion() {
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        doNothing().when(criterionPointsRepository).deleteByCategoryId(10L);
        when(criterionRepository.findById(1L)).thenReturn(Optional.of(criterion));

        CategoryCriterionPoints ccp = new CategoryCriterionPoints(category, criterion, 100);
        ccp.setId(100L);
        when(criterionPointsRepository.save(any(CategoryCriterionPoints.class))).thenReturn(Objects.requireNonNull(ccp));

        List<CategoryCriterionPointsDto> input = List.of(
                new CategoryCriterionPointsDto(null, 10L, 1L, "Innovación", 100)
        );

        List<CategoryCriterionPointsDto> result = categoryService.setCriterionPointsBulk(10L, input);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getWeightPercent()).isEqualTo(100);
        verify(criterionPointsRepository, times(1)).deleteByCategoryId(10L);
        verify(criterionPointsRepository, times(1)).save(any(CategoryCriterionPoints.class));
    }

    @Test
    @DisplayName("setCriterionPointsBulk → lanza excepción si weightPercent es negativo")
    void setCriterionPointsBulk_throwsException_whenNegativePoints() {
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        List<CategoryCriterionPointsDto> input = List.of(
                new CategoryCriterionPointsDto(null, 10L, 1L, "X", -5)
        );

        assertThatThrownBy(() -> categoryService.setCriterionPointsBulk(10L, input))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("non-negative");
    }

    @Test
    @DisplayName("setCriterionPointsBulk → lanza excepción si la suma de weightPercent no es 100")
    void setCriterionPointsBulk_throwsException_whenSumIsNot100() {
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        // 40 + 40 = 80 ≠ 100
        List<CategoryCriterionPointsDto> input = List.of(
                new CategoryCriterionPointsDto(null, 10L, 1L, "Innovación", 40),
                new CategoryCriterionPointsDto(null, 10L, 2L, "Calidad",    40)
        );

        assertThatThrownBy(() -> categoryService.setCriterionPointsBulk(10L, input))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("100");
    }

    @Test
    @DisplayName("setCriterionPointsBulk → acepta cuando la suma de weightPercent es exactamente 100")
    void setCriterionPointsBulk_savesPoints_whenSumIsExactly100() {
        Criterion criterion2 = new Criterion("Calidad");
        criterion2.setId(2L);

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        doNothing().when(criterionPointsRepository).deleteByCategoryId(10L);
        when(criterionRepository.findById(1L)).thenReturn(Optional.of(criterion));
        when(criterionRepository.findById(2L)).thenReturn(Optional.of(criterion2));

        CategoryCriterionPoints ccp1 = new CategoryCriterionPoints(category, criterion,  60);
        ccp1.setId(100L);
        CategoryCriterionPoints ccp2 = new CategoryCriterionPoints(category, criterion2, 40);
        ccp2.setId(101L);
        when(criterionPointsRepository.save(any(CategoryCriterionPoints.class)))
                .thenReturn(Objects.requireNonNull(ccp1)).thenReturn(Objects.requireNonNull(ccp2));

        // 60 + 40 = 100 ✓
        List<CategoryCriterionPointsDto> input = List.of(
                new CategoryCriterionPointsDto(null, 10L, 1L, "Innovación", 60),
                new CategoryCriterionPointsDto(null, 10L, 2L, "Calidad",    40)
        );

        List<CategoryCriterionPointsDto> result = categoryService.setCriterionPointsBulk(10L, input);

        assertThat(result).hasSize(2);
        verify(criterionPointsRepository).deleteByCategoryId(10L);
    }

    @Test
    @DisplayName("setCriterionPoints → lanza excepción si el total supera 100")
    void setCriterionPoints_throwsException_whenExceeds100() {
        Criterion criterion2 = new Criterion("Calidad");
        criterion2.setId(2L);

        // Ya hay un criterio con 70 puntos en la BD
        CategoryCriterionPoints existing = new CategoryCriterionPoints(category, criterion2, 70);
        existing.setId(200L);

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(criterionRepository.findById(1L)).thenReturn(Optional.of(criterion));
        when(criterionPointsRepository.findByCategoryIdAndCriterionId(10L, 1L)).thenReturn(Optional.empty());
        when(criterionPointsRepository.findByCategoryId(10L)).thenReturn(List.of(existing));

        // Intentar asignar 50 al criterio 1: 70 + 50 = 120 > 100
        assertThatThrownBy(() -> categoryService.setCriterionPoints(10L, 1L, 50))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("100");
    }

    // ── Validación de tiempos ──────────────────────────────────────────────

    @Test
    @DisplayName("setTimeInitial → lanza excepción si la fecha es anterior al inicio del evento")
    void setTimeInitial_throwsException_whenBeforeEventStart() {
        event.setTimeInitial(new Date(5000L));
        category.changeEndTime(new Date(8000L));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        // Fecha anterior al inicio del evento
        assertThatThrownBy(() -> categoryService.setTimeInitial(10L, new Date(1L)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("start time");
    }

    @Test
    @DisplayName("setTimeFinal → lanza excepción si la fecha fin es anterior a la de inicio")
    void setTimeFinal_throwsException_whenEndBeforeStart() {
        event.setTimeFinal(new Date(Long.MAX_VALUE));
        category.changeStartTime(new Date(5000L));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        // Fecha fin anterior a la de inicio de la categoría
        assertThatThrownBy(() -> categoryService.setTimeFinal(10L, new Date(1L)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("end time");
    }

    // ── setTotalPoints ─────────────────────────────────────────────────────

    @Test
    @DisplayName("setTotalPoints → configura puntos en categoría POPULAR_VOTE")
    void setTotalPoints_setsPointsForPopularVote() {
        category.changeVotingType(VotingType.POPULAR_VOTE);
        category.configureTotalPoints(10);
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenReturn(Objects.requireNonNull(category));

        CategoryDto result = categoryService.setTotalPoints(10L, 10);

        assertThat(result.getTotalPoints()).isEqualTo(10);
        assertThat(result.getVotingType()).isEqualTo(VotingType.POPULAR_VOTE);
    }

    @Test
    @DisplayName("setTotalPoints → lanza excepción si la categoría es JURY_EXPERT")
    void setTotalPoints_throwsException_whenJuryExpert() {
        category.changeVotingType(VotingType.JURY_EXPERT);
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> categoryService.setTotalPoints(10L, 10))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("POPULAR_VOTE");
    }

    @Test
    @DisplayName("setTotalPoints → lanza excepción si totalPoints es cero o negativo")
    void setTotalPoints_throwsException_whenZeroOrNegative() {
        assertThatThrownBy(() -> categoryService.setTotalPoints(10L, 0))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("positive");

        assertThatThrownBy(() -> categoryService.setTotalPoints(10L, -5))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("positive");
    }

    // ── setMaxVotesPerVoter ────────────────────────────────────────────────

    @Test
    @DisplayName("setMaxVotesPerVoter → configura límite de 3 de 5 en POPULAR_VOTE")
    void setMaxVotesPerVoter_setsLimitForPopularVote() {
        category.changeVotingType(VotingType.POPULAR_VOTE);
        category.limitVotesPerVoter(3);
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenReturn(Objects.requireNonNull(category));

        CategoryDto result = categoryService.setMaxVotesPerVoter(10L, 3);

        assertThat(result.getMaxVotesPerVoter()).isEqualTo(3);
    }

    @Test
    @DisplayName("setMaxVotesPerVoter → lanza excepción si la categoría es JURY_EXPERT")
    void setMaxVotesPerVoter_throwsException_whenJuryExpert() {
        category.changeVotingType(VotingType.JURY_EXPERT);
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> categoryService.setMaxVotesPerVoter(10L, 3))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("POPULAR_VOTE");
    }

    @Test
    @DisplayName("setMaxVotesPerVoter → lanza excepción si el valor es cero o negativo")
    void setMaxVotesPerVoter_throwsException_whenZeroOrNegative() {
        assertThatThrownBy(() -> categoryService.setMaxVotesPerVoter(10L, 0))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("positive");
    }

    // ── Guard JURY_EXPERT en setCriterionPointsBulk ────────────────────────

    @Test
    @DisplayName("setCriterionPointsBulk → lanza excepción si la categoría es POPULAR_VOTE")
    void setCriterionPointsBulk_throwsException_whenPopularVote() {
        category.changeVotingType(VotingType.POPULAR_VOTE);
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        List<CategoryCriterionPointsDto> input = List.of(
                new CategoryCriterionPointsDto(null, 10L, 1L, "Innovación", 100)
        );

        assertThatThrownBy(() -> categoryService.setCriterionPointsBulk(10L, input))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("JURY_EXPERT");
    }
}
