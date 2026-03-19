package com.votify.service;

import com.votify.dto.CriterionDto;
import com.votify.entity.Criterion;
import com.votify.persistence.CriterionRepository;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("CriterionService — Tests unitarios")
class CriterionServiceTest {

    @Mock
    private CriterionRepository criterionRepository;

    @InjectMocks
    private CriterionService criterionService;

    private Criterion criterion;

    @BeforeEach
    void setUp() {
        criterion = new Criterion("Innovación");
        criterion.setId(1L);
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

    // ── create ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create → guarda y retorna DTO con id generado")
    void create_savesAndReturnsDto() {
        CriterionDto dto = new CriterionDto(null, "Viabilidad");
        Criterion saved = new Criterion("Viabilidad");
        saved.setId(3L);
        when(criterionRepository.save(any(Criterion.class))).thenReturn(saved);

        CriterionDto result = criterionService.create(dto);

        assertThat(result.getId()).isEqualTo(3L);
        assertThat(result.getName()).isEqualTo("Viabilidad");
        verify(criterionRepository, times(1)).save(any(Criterion.class));
    }

    @Test
    @DisplayName("create → llama a save exactamente una vez")
    void create_callsSaveOnce() {
        CriterionDto dto = new CriterionDto(null, "Presentación");
        Criterion saved = new Criterion("Presentación");
        saved.setId(4L);
        when(criterionRepository.save(any())).thenReturn(saved);

        criterionService.create(dto);

        verify(criterionRepository, times(1)).save(any());
    }

    // ── update ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update → modifica nombre y retorna DTO actualizado")
    void update_changesNameAndReturnUpdatedDto() {
        when(criterionRepository.findById(1L)).thenReturn(Optional.of(criterion));
        Criterion updated = new Criterion("Innovación Digital");
        updated.setId(1L);
        when(criterionRepository.save(any(Criterion.class))).thenReturn(updated);

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
    @DisplayName("delete → llama a deleteById con el id correcto")
    void delete_callsDeleteById() {
        doNothing().when(criterionRepository).deleteById(1L);

        criterionService.delete(1L);

        verify(criterionRepository, times(1)).deleteById(1L);
    }
}
