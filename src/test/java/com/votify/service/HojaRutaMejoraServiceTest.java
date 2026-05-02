package com.votify.service;

import com.votify.dto.HojaRutaMejoraDto;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
@DisplayName("HojaRutaMejoraService — Tests unitarios")
class HojaRutaMejoraServiceTest {

    @Mock private HojaRutaMejoraRepository hojaRutaRepository;
    @Mock private UserRepository userRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private EvaluacionRepository evaluacionRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private EventJuryRepository eventJuryRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private VotingRepository votingRepository;

    @InjectMocks
    private HojaRutaMejoraService hojaRutaService;

    private User competitor;
    private Category category;
    private Criterion criterion;

    @BeforeEach
    void setUp() {
        competitor = new User("Ana García", "ana@test.com", null);
        competitor.setId(1L);

        Event event = new Event("Hackathon UPV 2026");
        event.setId(10L);

        category = new Category("Innovación Tecnológica", event);
        category.setId(5L);

        criterion = new Criterion("Presentacion");
        criterion.setId(3L);

        // Stubs lenient para los nuevos repos (no todos los tests los usan,
        // pero el servicio los invoca cuando genera/getOrGenera)
        lenient().when(projectRepository.findByCompetitorId(anyLong())).thenReturn(List.of());
        lenient().when(commentRepository.findByProjectId(anyLong())).thenReturn(List.of());
        lenient().when(votingRepository.findByProjectIdAndComentarioIsNotNull(anyLong()))
                 .thenReturn(List.of());
    }

    // ── getOrGenerar ─────────────────────────────────────────────

    @Test
    @DisplayName("getOrGenerar: devuelve hoja existente sin regenerar")
    void getOrGenerar_cuandoExiste_devuelveExistente() {
        HojaRutaMejora existente = new HojaRutaMejora(competitor, category,
                "Resumen previo", false);
        existente.setId(99L);

        when(hojaRutaRepository.findByCompetitorIdAndCategoryId(1L, 5L))
                .thenReturn(Optional.of(existente));
        when(evaluacionRepository.findByCategoryIdAndCompetitorId(5L, 1L))
                .thenReturn(List.of());

        HojaRutaMejoraDto result = hojaRutaService.getOrGenerar(1L, 5L);

        assertThat(result.getId()).isEqualTo(99L);
        assertThat(result.getResumenGeneral()).isEqualTo("Resumen previo");
        assertThat(result.isGeneradoIa()).isFalse();
        // No debe llamar a save() si ya existe
        verify(hojaRutaRepository, never()).save(any());
    }

    @Test
    @DisplayName("getOrGenerar: genera nueva hoja cuando no existe")
    void getOrGenerar_cuandoNoExiste_genera() {
        when(hojaRutaRepository.findByCompetitorIdAndCategoryId(1L, 5L))
                .thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(competitor));
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(evaluacionRepository.findByCategoryIdAndCompetitorId(5L, 1L))
                .thenReturn(List.of());

        HojaRutaMejora saved = new HojaRutaMejora(competitor, category, "Generado", false);
        saved.setId(1L);
        when(hojaRutaRepository.save(any())).thenReturn(saved);

        HojaRutaMejoraDto result = hojaRutaService.getOrGenerar(1L, 5L);

        assertThat(result).isNotNull();
        verify(hojaRutaRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("getOrGenerar global: sin categoryId busca por categoría nula")
    void getOrGenerar_sinCategoria_buscaPorCategoriaNula() {
        HojaRutaMejora existente = new HojaRutaMejora(competitor, null, "Global", false);
        existente.setId(50L);

        when(hojaRutaRepository.findByCompetitorIdAndCategoryIsNull(1L))
                .thenReturn(Optional.of(existente));
        when(evaluacionRepository.findByCompetitorId(1L)).thenReturn(List.of());

        HojaRutaMejoraDto result = hojaRutaService.getOrGenerar(1L, null);

        assertThat(result.getCategoryId()).isNull();
        assertThat(result.getResumenGeneral()).isEqualTo("Global");
        verify(hojaRutaRepository, never()).save(any());
    }

    // ── generar ──────────────────────────────────────────────────

    @Test
    @DisplayName("generar: persiste nueva hoja de ruta y devuelve DTO")
    void generar_sinComentarios_creaHojaConMensajeVacio() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(competitor));
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(evaluacionRepository.findByCategoryIdAndCompetitorId(5L, 1L))
                .thenReturn(List.of());

        HojaRutaMejora saved = new HojaRutaMejora(competitor, category,
                "Aún no hay comentarios de expertos registrados para este competidor en esta categoría.", false);
        saved.setId(2L);
        when(hojaRutaRepository.save(any())).thenReturn(saved);

        HojaRutaMejoraDto result = hojaRutaService.generar(1L, 5L);

        assertThat(result.getCompetitorId()).isEqualTo(1L);
        assertThat(result.getCategoryId()).isEqualTo(5L);
        assertThat(result.getAreasMejora()).isEmpty();
        assertThat(result.isGeneradoIa()).isFalse();
        verify(hojaRutaRepository).save(any());
    }

    @Test
    @DisplayName("generar: agrupa comentarios por criterio en áreasMejora")
    void generar_conComentarios_agrupaPorCriterio() {
        User experto = new User("Prof. Martínez", "prof@test.com", null);
        experto.setId(7L);

        EvaluacionComentario eval1 = new EvaluacionComentario(
                experto, competitor, category, criterion, null,
                "{\"texto\": \"Muy buena presentación, pero falta análisis de mercado.\"}");
        EvaluacionComentario eval2 = new EvaluacionComentario(
                experto, competitor, category, criterion, null,
                "{\"texto\": \"La demo estuvo muy fluida.\"}");

        when(userRepository.findById(1L)).thenReturn(Optional.of(competitor));
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(evaluacionRepository.findByCategoryIdAndCompetitorId(5L, 1L))
                .thenReturn(List.of(eval1, eval2));

        HojaRutaMejora saved = new HojaRutaMejora(competitor, category,
                "Ana García ha recibido 2 comentarios de expertos en 1 criterio.", false);
        saved.setId(3L);
        when(hojaRutaRepository.save(any())).thenReturn(saved);

        HojaRutaMejoraDto result = hojaRutaService.generar(1L, 5L);

        assertThat(result.getAreasMejora()).hasSize(1);
        assertThat(result.getAreasMejora().get(0).getCriterioNombre()).isEqualTo("Presentacion");
        assertThat(result.getAreasMejora().get(0).getComentarios()).hasSize(2);
        assertThat(result.getAreasMejora().get(0).getComentarios().get(0).getTexto())
                .isEqualTo("Muy buena presentación, pero falta análisis de mercado.");
    }

    @Test
    @DisplayName("generar: ignora evaluaciones que no son EvaluacionComentario")
    void generar_ignoraEvaluacionesNoComentario() {
        User experto = new User("Experto", "exp@test.com", null);
        experto.setId(8L);

        // Solo EvaluacionNumerica — no debe aparecer en áreas de mejora
        EvaluacionNumerica numerica = new EvaluacionNumerica(
                experto, competitor, category, criterion, null, "{\"valor\": 8.5}");

        when(userRepository.findById(1L)).thenReturn(Optional.of(competitor));
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(evaluacionRepository.findByCategoryIdAndCompetitorId(5L, 1L))
                .thenReturn(List.of(numerica));

        HojaRutaMejora saved = new HojaRutaMejora(competitor, category,
                "Aún no hay comentarios de expertos registrados para este competidor en esta categoría.", false);
        saved.setId(4L);
        when(hojaRutaRepository.save(any())).thenReturn(saved);

        HojaRutaMejoraDto result = hojaRutaService.generar(1L, 5L);

        assertThat(result.getAreasMejora()).isEmpty();
    }

    @Test
    @DisplayName("generar: lanza excepción si el competidor no existe")
    void generar_competidorNoExiste_lanzaExcepcion() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hojaRutaService.generar(99L, 5L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Competitor not found");
    }

    @Test
    @DisplayName("generar: lanza excepción si la categoría no existe")
    void generar_categoriaNoExiste_lanzaExcepcion() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(competitor));
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hojaRutaService.generar(1L, 99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Category not found");
    }

    @Test
    @DisplayName("generar: borra hoja de ruta previa antes de guardar la nueva")
    void generar_borraHojaPreviaAntesDeGuardar() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(competitor));
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(evaluacionRepository.findByCategoryIdAndCompetitorId(5L, 1L))
                .thenReturn(List.of());

        HojaRutaMejora saved = new HojaRutaMejora(competitor, category, "Nueva", false);
        saved.setId(5L);
        when(hojaRutaRepository.save(any())).thenReturn(saved);

        hojaRutaService.generar(1L, 5L);

        verify(hojaRutaRepository).deleteByCompetitorIdAndCategoryId(1L, 5L);
        verify(hojaRutaRepository).save(any());
    }

    @Test
    @DisplayName("generar global: sin categoryId no consulta categoryRepository")
    void generar_sinCategoryId_noConsultaCategoria() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(competitor));
        when(evaluacionRepository.findByCompetitorId(1L)).thenReturn(List.of());

        HojaRutaMejora saved = new HojaRutaMejora(competitor, null, "Global", false);
        saved.setId(6L);
        when(hojaRutaRepository.save(any())).thenReturn(saved);

        hojaRutaService.generar(1L, null);

        verify(categoryRepository, never()).findById(any());
        verify(hojaRutaRepository).deleteByCompetitorIdAndCategoryIsNull(1L);
    }

    // ── clasificar() — Opción C: clasificación backend de comentariosAdicionales ──

    @Test
    @DisplayName("clasificar: comentario con keyword de mejora → esMejora = true")
    void clasificar_comentarioConKeywordMejora_esClasificadoComoMejora() {
        // Preparar un proyecto mock ligado al competidor
        Project proyecto = mock(Project.class);
        when(proyecto.getId()).thenReturn(20L);
        when(proyecto.getEvent()).thenReturn(null); // sin evento → origen siempre Popular

        when(userRepository.findById(1L)).thenReturn(Optional.of(competitor));
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(evaluacionRepository.findByCategoryIdAndCompetitorId(5L, 1L)).thenReturn(List.of());
        when(projectRepository.findByCompetitorId(1L)).thenReturn(List.of(proyecto));

        // Comentario con keyword "pero falta" → debe clasificarse como mejora
        User autor = new User("Jurado1", "j1@test.com", null);
        Comment comentarioMejora = new Comment(
                "La presentación estuvo bien, pero falta análisis de mercado.", autor, proyecto);
        when(commentRepository.findByProjectId(20L)).thenReturn(List.of(comentarioMejora));
        when(votingRepository.findByProjectIdAndComentarioIsNotNull(20L)).thenReturn(List.of());

        HojaRutaMejora saved = new HojaRutaMejora(competitor, category, "resumen", false);
        saved.setId(7L);
        when(hojaRutaRepository.save(any())).thenReturn(saved);

        HojaRutaMejoraDto result = hojaRutaService.generar(1L, 5L);

        assertThat(result.getComentariosAdicionales()).hasSize(1);
        assertThat(result.getComentariosAdicionales().get(0).esMejora()).isTrue();
        assertThat(result.getComentariosAdicionales().get(0).autor()).isEqualTo("Jurado1");
    }

    @Test
    @DisplayName("clasificar: comentario sin keywords → esMejora = false")
    void clasificar_comentarioPositivo_esClasificadoComoNoMejora() {
        Project proyecto = mock(Project.class);
        when(proyecto.getId()).thenReturn(21L);
        when(proyecto.getEvent()).thenReturn(null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(competitor));
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(evaluacionRepository.findByCategoryIdAndCompetitorId(5L, 1L)).thenReturn(List.of());
        when(projectRepository.findByCompetitorId(1L)).thenReturn(List.of(proyecto));

        // Comentario positivo, sin ninguna keyword negativa
        User autor = new User("Votante1", "v1@test.com", null);
        Comment comentarioPositivo = new Comment(
                "Excelente proyecto, muy bien ejecutado y presentado.", autor, proyecto);
        when(commentRepository.findByProjectId(21L)).thenReturn(List.of(comentarioPositivo));
        when(votingRepository.findByProjectIdAndComentarioIsNotNull(21L)).thenReturn(List.of());

        HojaRutaMejora saved = new HojaRutaMejora(competitor, category, "resumen", false);
        saved.setId(8L);
        when(hojaRutaRepository.save(any())).thenReturn(saved);

        HojaRutaMejoraDto result = hojaRutaService.generar(1L, 5L);

        assertThat(result.getComentariosAdicionales()).hasSize(1);
        assertThat(result.getComentariosAdicionales().get(0).esMejora()).isFalse();
    }

    @Test
    @DisplayName("clasificar: comentariosAdicionales nunca es null en el DTO")
    void clasificar_comentariosAdicionales_nuncaEsNullEnDto() {
        // Sin proyectos → recogerComentariosAdicionales devuelve lista vacía
        // El DTO debe incluir una lista vacía, nunca null
        when(userRepository.findById(1L)).thenReturn(Optional.of(competitor));
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(evaluacionRepository.findByCategoryIdAndCompetitorId(5L, 1L)).thenReturn(List.of());
        // projectRepository ya tiene stub lenient en setUp() → devuelve List.of()

        HojaRutaMejora saved = new HojaRutaMejora(competitor, category, "sin datos", false);
        saved.setId(9L);
        when(hojaRutaRepository.save(any())).thenReturn(saved);

        HojaRutaMejoraDto result = hojaRutaService.generar(1L, 5L);

        assertThat(result.getComentariosAdicionales())
                .isNotNull()
                .isEmpty();
    }
}
