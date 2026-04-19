package com.votify.service;

import com.votify.dto.EvaluacionDto;
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

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
@DisplayName("EvaluacionService — Tests unitarios")
class EvaluacionServiceTest {

    @Mock private EvaluacionRepository evaluacionRepository;
    @Mock private UserRepository userRepository;
    @Mock private CompetitorRepository competitorRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private CriterionRepository criterionRepository;

    @InjectMocks
    private EvaluacionService evaluacionService;

    private User evaluador;
    private Competitor competitor;
    private Category category;
    private Event event;

    @BeforeEach
    void setUp() {
        event = new Event("Hackathon 2026");
        event.setId(1L);

        evaluador = new User("Admin", "admin@test.com", null);
        evaluador.setId(10L);

        competitor = new Competitor("Carlos", "carlos@test.com", null);
        competitor.setId(20L);

        category = new Category("Proyectos Sociales", event);
        category.setId(30L);
    }

    // ── findAll ───────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll → retorna todas las evaluaciones")
    void findAll_returnsAll() {
        EvaluacionNumerica e1 = new EvaluacionNumerica(evaluador, competitor, category, null, 1.0, "{\"valores\":[8]}");
        e1.setId(1L);
        e1.setCreatedAt(new Date());

        when(evaluacionRepository.findAll()).thenReturn(List.of(e1));

        List<EvaluacionDto> result = evaluacionService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTipo()).isEqualTo("NUMERICA");
    }

    // ── findById ──────────────────────────────────────────────────────

    @Test
    @DisplayName("findById → retorna evaluación existente")
    void findById_returnsExisting() {
        EvaluacionChecklist eval = new EvaluacionChecklist(evaluador, competitor, category, null, 1.0, "{\"items\":[true,false]}");
        eval.setId(5L);
        eval.setCreatedAt(new Date());

        when(evaluacionRepository.findById(5L)).thenReturn(Optional.of(eval));

        EvaluacionDto result = evaluacionService.findById(5L);

        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getTipo()).isEqualTo("CHECKLIST");
        assertThat(result.getScore()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("findById → lanza excepción si no existe")
    void findById_throwsWhenNotFound() {
        when(evaluacionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> evaluacionService.findById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    // ── create ────────────────────────────────────────────────────────

    @Test
    @DisplayName("create NUMERICA → usa factory method y calcula score")
    void create_numerica_usesFactoryMethod() {
        EvaluacionDto dto = new EvaluacionDto("NUMERICA", 10L, 20L, 30L, null, 1.0, "{\"valores\":[8,7,9]}");

        when(userRepository.findById(10L)).thenReturn(Optional.of(evaluador));
        when(competitorRepository.findById(20L)).thenReturn(Optional.of(competitor));
        when(categoryRepository.findById(30L)).thenReturn(Optional.of(category));
        when(evaluacionRepository.save(any(Evaluacion.class))).thenAnswer(inv -> {
            Evaluacion e = inv.getArgument(0);
            e.setId(100L);
            return e;
        });

        EvaluacionDto result = evaluacionService.create(dto);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getTipo()).isEqualTo("NUMERICA");
        assertThat(result.getScore()).isEqualTo(24.0);
        verify(evaluacionRepository).save(any(EvaluacionNumerica.class));
    }

    @Test
    @DisplayName("create CHECKLIST → usa factory method correcto")
    void create_checklist_usesFactoryMethod() {
        EvaluacionDto dto = new EvaluacionDto("CHECKLIST", 10L, 20L, 30L, null, 1.0, "{\"items\":[true,true,false]}");

        when(userRepository.findById(10L)).thenReturn(Optional.of(evaluador));
        when(competitorRepository.findById(20L)).thenReturn(Optional.of(competitor));
        when(categoryRepository.findById(30L)).thenReturn(Optional.of(category));
        when(evaluacionRepository.save(any(Evaluacion.class))).thenAnswer(inv -> {
            Evaluacion e = inv.getArgument(0);
            e.setId(101L);
            return e;
        });

        EvaluacionDto result = evaluacionService.create(dto);

        assertThat(result.getTipo()).isEqualTo("CHECKLIST");
        assertThat(result.getScore()).isCloseTo(66.67, within(0.01));
    }

    @Test
    @DisplayName("create COMENTARIO → score es null (cualitativa)")
    void create_comentario_scoreIsNull() {
        EvaluacionDto dto = new EvaluacionDto("COMENTARIO", 10L, 20L, 30L, null, 0.0, "{\"texto\":\"Buen trabajo\"}");

        when(userRepository.findById(10L)).thenReturn(Optional.of(evaluador));
        when(competitorRepository.findById(20L)).thenReturn(Optional.of(competitor));
        when(categoryRepository.findById(30L)).thenReturn(Optional.of(category));
        when(evaluacionRepository.save(any(Evaluacion.class))).thenAnswer(inv -> {
            Evaluacion e = inv.getArgument(0);
            e.setId(102L);
            return e;
        });

        EvaluacionDto result = evaluacionService.create(dto);

        assertThat(result.getTipo()).isEqualTo("COMENTARIO");
        assertThat(result.getScore()).isNull();
    }

    @Test
    @DisplayName("create → lanza excepción con tipo inválido")
    void create_invalidType_throws() {
        EvaluacionDto dto = new EvaluacionDto("INVALIDO", 10L, 20L, 30L, null, 1.0, "{}");

        assertThatThrownBy(() -> evaluacionService.create(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no válido");
    }

    @Test
    @DisplayName("create → lanza excepción con peso negativo")
    void create_negativePeso_throws() {
        EvaluacionDto dto = new EvaluacionDto("NUMERICA", 10L, 20L, 30L, null, -5.0, "{\"valores\":[1]}");

        assertThatThrownBy(() -> evaluacionService.create(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("negativo");
    }

    @Test
    @DisplayName("create → lanza excepción si evaluador no existe")
    void create_evaluadorNotFound_throws() {
        EvaluacionDto dto = new EvaluacionDto("NUMERICA", 999L, 20L, 30L, null, 1.0, "{\"valores\":[1]}");

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> evaluacionService.create(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    // ── delete ────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete → elimina evaluación existente")
    void delete_existingEvaluacion() {
        when(evaluacionRepository.existsById(1L)).thenReturn(true);
        doNothing().when(evaluacionRepository).deleteById(1L);

        evaluacionService.delete(1L);

        verify(evaluacionRepository).deleteById(1L);
    }

    @Test
    @DisplayName("delete → lanza excepción si no existe")
    void delete_nonExisting_throws() {
        when(evaluacionRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> evaluacionService.delete(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    // ── queries ───────────────────────────────────────────────────────

    @Test
    @DisplayName("findByCategory → retorna evaluaciones de la categoría")
    void findByCategory_returnsFiltered() {
        EvaluacionNumerica e1 = new EvaluacionNumerica(evaluador, competitor, category, null, 1.0, "{\"valores\":[5]}");
        e1.setId(1L);
        e1.setCreatedAt(new Date());

        when(evaluacionRepository.findByCategoryId(30L)).thenReturn(List.of(e1));

        List<EvaluacionDto> result = evaluacionService.findByCategory(30L);

        assertThat(result).hasSize(1);
    }
}
