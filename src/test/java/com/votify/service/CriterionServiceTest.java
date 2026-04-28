package com.votify.service;

import com.votify.dto.CriterionDto;
import com.votify.entity.Category;
import com.votify.entity.Criterion;
import com.votify.entity.Event;
import com.votify.persistence.CategoryCriterionPointsRepository;
import com.votify.persistence.CategoryRepository;
import com.votify.persistence.CriterionRepository;
import com.votify.persistence.EvaluacionRepository;
import com.votify.persistence.VotingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
@DisplayName("CriterionService — Tests unitarios")
class CriterionServiceTest {

    @Mock
    private CriterionRepository criterionRepository;

    @Mock
    private CategoryCriterionPointsRepository criterionPointsRepository;

    @Mock
    private VotingRepository votingRepository;

    @Mock
    private EvaluacionRepository evaluacionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CriterionService criterionService;

    private Criterion criterion;
    private Category category;

    @BeforeEach
    void setUp() {
        Event event = new Event("Hackathon 2026");
        event.setId(1L);

        category = new Category("Jurado Experto", event);
        category.setId(10L);

        criterion = new Criterion("Innovación");
        criterion.setId(1L);
        criterion.setCategory(category);
    }

    // ── findAll ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll → retorna lista de DTOs correctamente")
    void findAll_returnsListOfDtos() {
        Criterion c2 = new Criterion("Calidad Técnica");
        c2.setId(2L);
        when(criterionRepository.findAll()).thenReturn(List.of(criterion, c2));

        List<CriterionDto> result = criterionService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Innovación");
        assertThat(result.get(1).getName()).isEqualTo("Calidad Técnica");
    }

    @Test
    @DisplayName("findAll → retorna lista vacía cuando no hay criterios")
    void findAll_returnsEmptyList_whenNoCriteria() {
        when(criterionRepository.findAll()).thenReturn(List.of());
        assertThat(criterionService.findAll()).isEmpty();
    }

    // ── findById ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById → retorna DTO cuando existe")
    void findById_returnsDto_whenFound() {
        when(criterionRepository.findById(1L)).thenReturn(Optional.of(criterion));

        CriterionDto result = criterionService.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Innovación");
    }

    @Test
    @DisplayName("findById → lanza excepción cuando no existe")
    void findById_throwsException_whenNotFound() {
        when(criterionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> criterionService.findById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    // ── findByCategoryId ───────────────────────────────────────────────────

    @Test
    @DisplayName("findByCategoryId → retorna criterios de la categoría")
    void findByCategoryId_returnsCriteriaForCategory() {
        Criterion c2 = new Criterion("Calidad");
        c2.setId(2L);
        c2.setCategory(category);
        when(criterionRepository.findByCategoryId(10L)).thenReturn(List.of(criterion, c2));

        List<CriterionDto> result = criterionService.findByCategoryId(10L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCategoryId()).isEqualTo(10L);
        assertThat(result.get(1).getCategoryId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("findByCategoryId → retorna lista vacía si no hay criterios en esa categoría")
    void findByCategoryId_returnsEmpty_whenNoCriteriaForCategory() {
        when(criterionRepository.findByCategoryId(99L)).thenReturn(List.of());

        assertThat(criterionService.findByCategoryId(99L)).isEmpty();
    }

    // ── create ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create → guarda y retorna DTO con categoría asignada")
    void create_savesAndReturnsDto() {
        CriterionDto dto = new CriterionDto(null, "Viabilidad", 10L);
        Criterion saved = new Criterion("Viabilidad");
        saved.setId(3L);
        saved.setCategory(category);
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(criterionRepository.save(any(Criterion.class))).thenReturn(Objects.requireNonNull(saved));

        CriterionDto result = criterionService.create(dto);

        assertThat(result.getId()).isEqualTo(3L);
        assertThat(result.getName()).isEqualTo("Viabilidad");
        assertThat(result.getCategoryId()).isEqualTo(10L);
        verify(criterionRepository, times(1)).save(any(Criterion.class));
    }

    @Test
    @DisplayName("create → llama a save exactamente una vez")
    void create_callsSaveOnce() {
        CriterionDto dto = new CriterionDto(null, "Presentación", 10L);
        Criterion saved = new Criterion("Presentación");
        saved.setId(4L);
        saved.setCategory(category);
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(criterionRepository.save(any())).thenReturn(Objects.requireNonNull(saved));

        criterionService.create(dto);

        verify(criterionRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("create → lanza excepción si categoryId es nulo")
    void create_throwsException_whenCategoryIdIsNull() {
        CriterionDto dto = new CriterionDto(null, "Viabilidad", null);

        assertThatThrownBy(() -> criterionService.create(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("categoría");
    }

    @Test
    @DisplayName("create → lanza excepción si la categoría no existe")
    void create_throwsException_whenCategoryNotFound() {
        CriterionDto dto = new CriterionDto(null, "Viabilidad", 99L);
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> criterionService.create(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    // ── update ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update → modifica nombre y retorna DTO actualizado")
    void update_changesNameAndReturnUpdatedDto() {
        when(criterionRepository.findById(1L)).thenReturn(Optional.of(criterion));
        Criterion updated = new Criterion("Innovación Digital");
        updated.setId(1L);
        when(criterionRepository.save(any(Criterion.class))).thenReturn(Objects.requireNonNull(updated));

        CriterionDto result = criterionService.update(1L, new CriterionDto(1L, "Innovación Digital"));

        assertThat(result.getName()).isEqualTo("Innovación Digital");
    }

    @Test
    @DisplayName("update → lanza excepción si el criterio no existe")
    void update_throwsException_whenNotFound() {
        when(criterionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> criterionService.update(99L, new CriterionDto(99L, "X")))
                .isInstanceOf(RuntimeException.class);
    }

    // ── delete ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete → cascade deletes related records then deletes criterion")
    void delete_callsDeleteById() {
        when(criterionRepository.existsById(1L)).thenReturn(true);
        doNothing().when(votingRepository).deleteByCriterionId(1L);
        doNothing().when(evaluacionRepository).deleteByCriterionId(1L);
        doNothing().when(criterionPointsRepository).deleteByCriterionId(1L);
        doNothing().when(criterionRepository).deleteById(1L);

        criterionService.delete(1L);

        verify(votingRepository, times(1)).deleteByCriterionId(1L);
        verify(evaluacionRepository, times(1)).deleteByCriterionId(1L);
        verify(criterionPointsRepository, times(1)).deleteByCriterionId(1L);
        verify(criterionRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("delete → lanza excepcion si criterio no existe")
    void delete_throwsException_whenNotFound() {
        when(criterionRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> criterionService.delete(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }
}
