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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService — Tests unitarios")
class CategoryServiceTest {

    @Mock private CategoryRepository               categoryRepository;
    @Mock private EventRepository                  eventRepository;
    @Mock private CriterionRepository              criterionRepository;
    @Mock private CategoryCriterionPointsRepository criterionPointsRepository;

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
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);

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
        category.setVotingType(VotingType.JURY_EXPERT);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        CategoryDto result = categoryService.setVotingType(10L, VotingType.JURY_EXPERT);

        assertThat(result.getVotingType()).isEqualTo(VotingType.JURY_EXPERT);
    }

    @Test
    @DisplayName("setVotingType → asigna POPULAR_VOTE correctamente")
    void setVotingType_setsPopularVote() {
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        category.setVotingType(VotingType.POPULAR_VOTE);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

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

        CategoryCriterionPoints ccp = new CategoryCriterionPoints(category, criterion, 30);
        ccp.setId(100L);
        when(criterionPointsRepository.save(any(CategoryCriterionPoints.class))).thenReturn(ccp);

        List<CategoryCriterionPointsDto> input = List.of(
                new CategoryCriterionPointsDto(null, 10L, 1L, "Innovación", 30)
        );

        List<CategoryCriterionPointsDto> result = categoryService.setCriterionPointsBulk(10L, input);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMaxPoints()).isEqualTo(30);
        verify(criterionPointsRepository, times(1)).deleteByCategoryId(10L);
        verify(criterionPointsRepository, times(1)).save(any(CategoryCriterionPoints.class));
    }

    @Test
    @DisplayName("setCriterionPointsBulk → lanza excepción si maxPoints es negativo")
    void setCriterionPointsBulk_throwsException_whenNegativePoints() {
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        doNothing().when(criterionPointsRepository).deleteByCategoryId(10L);

        List<CategoryCriterionPointsDto> input = List.of(
                new CategoryCriterionPointsDto(null, 10L, 1L, "X", -5)
        );

        assertThatThrownBy(() -> categoryService.setCriterionPointsBulk(10L, input))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("non-negative");
    }

    // ── Validación de tiempos ──────────────────────────────────────────────

    @Test
    @DisplayName("setTimeInitial → lanza excepción si la fecha es anterior al inicio del evento")
    void setTimeInitial_throwsException_whenBeforeEventStart() {
        event.setTimeInitial(new Date(5000L));
        category.setTimeFinal(new Date(8000L));
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
        category.setTimeInitial(new Date(5000L));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        // Fecha fin anterior a la de inicio de la categoría
        assertThatThrownBy(() -> categoryService.setTimeFinal(10L, new Date(1L)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("end time");
    }
}
