package com.votify.service;

import com.votify.dto.AreaMejoraDto;
import com.votify.dto.ComentarioExpertoDto;
import com.votify.dto.HojaRutaMejoraDto;
import com.votify.entity.*;
import com.votify.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.votify.entity.User;
import com.votify.persistence.UserRepository;
import java.util.*;

/**
 * Servicio para generar y recuperar la hoja de ruta de mejora de un competidor.
 *
 * Implementación parcial (sin IA):
 *   - Recoge todos los EvaluacionComentario del competidor (filtrados por categoría si se indica).
 *   - Agrupa los comentarios por criterio → Lista de AreaMejoraDto.
 *   - Genera un resumen automático con el recuento de evaluaciones y criterios.
 *   - Persiste la cabecera en HojaRutaMejora (generadoIa = false).
 *
 * Extensión futura con IA:
 *   - Reemplazar buildResumenAutomatico() por una llamada al modelo LLM.
 *   - Setear generadoIa = true en la entidad guardada.
 */
@Service
public class HojaRutaMejoraService {

    private final HojaRutaMejoraRepository hojaRutaRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EvaluacionRepository evaluacionRepository;

    public HojaRutaMejoraService(HojaRutaMejoraRepository hojaRutaRepository,
                                  UserRepository userRepository,
                                  CategoryRepository categoryRepository,
                                  EvaluacionRepository evaluacionRepository) {
        this.hojaRutaRepository = hojaRutaRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.evaluacionRepository = evaluacionRepository;
    }

    /**
     * Recupera la hoja de ruta existente o la genera si no existe todavía.
     *
     * @param competitorId ID del competidor
     * @param categoryId   ID de la categoría (null = global)
     */
    public HojaRutaMejoraDto getOrGenerar(Long competitorId, Long categoryId) {
        Optional<HojaRutaMejora> existing = findExisting(competitorId, categoryId);
        if (existing.isPresent()) {
            HojaRutaMejora entidad = existing.get();
            List<AreaMejoraDto> areas = buildAreasMejora(competitorId, categoryId);
            return toDto(entidad, areas);
        }
        return generar(competitorId, categoryId);
    }

    /**
     * Genera (o regenera) la hoja de ruta, sobrescribiendo la anterior.
     *
     * @param competitorId ID del competidor
     * @param categoryId   ID de la categoría (null = global)
     */
    @Transactional
    public HojaRutaMejoraDto generar(Long competitorId, Long categoryId) {
        User competitor = userRepository.findById(competitorId)
                .orElseThrow(() -> new RuntimeException("Competitor not found: " + competitorId));

        Category category = null;
        if (categoryId != null) {
            category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Category not found: " + categoryId));
        }

        // Recoger y agrupar comentarios de expertos
        List<AreaMejoraDto> areas = buildAreasMejora(competitorId, categoryId);

        // Generar resumen automático
        String resumen = buildResumenAutomatico(competitor, areas, categoryId);

        // Borrar hoja de ruta anterior si existe
        deleteExisting(competitorId, categoryId);

        // Persistir nueva hoja de ruta
        HojaRutaMejora entidad = new HojaRutaMejora(competitor, category, resumen, false);
        entidad = hojaRutaRepository.save(entidad);

        return toDto(entidad, areas);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Métodos privados
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Recoge las EvaluacionComentario del competidor (y categoría si se indica),
     * las agrupa por criterio y construye la lista de AreaMejoraDto.
     */
    private List<AreaMejoraDto> buildAreasMejora(Long competitorId, Long categoryId) {
        List<Evaluacion> evaluaciones = (categoryId != null)
                ? evaluacionRepository.findByCategoryIdAndCompetitorId(categoryId, competitorId)
                : evaluacionRepository.findByCompetitorId(competitorId);

        // Filtrar solo tipo COMENTARIO
        Map<Long, AreaMejoraDto> porCriterio = new LinkedHashMap<>();

        for (Evaluacion ev : evaluaciones) {
            if (!(ev instanceof EvaluacionComentario)) continue;

            Criterion criterion = ev.getCriterion();
            Long criterioId = criterion != null ? criterion.getId() : null;
            String criterioNombre = criterion != null ? criterion.getName() : "Sin criterio";

            porCriterio.computeIfAbsent(criterioId,
                    k -> new AreaMejoraDto(criterioId, criterioNombre, new ArrayList<>()));

            String texto = extraerTexto(ev.getDatos());
            Long evalId = ev.getEvaluador() != null ? ev.getEvaluador().getId() : null;
            String evalNombre = ev.getEvaluador() != null ? ev.getEvaluador().getName() : "Experto";

            porCriterio.get(criterioId)
                    .getComentarios()
                    .add(new ComentarioExpertoDto(evalId, evalNombre, texto));
        }

        return new ArrayList<>(porCriterio.values());
    }

    /**
     * Genera el resumen de la hoja de ruta leyendo el contenido real de los comentarios.
     * Opción B (sin IA): plantilla enriquecida que cita fragmentos de cada evaluador
     * agrupados por criterio, con aperturas variadas y cierre contextual.
     * Sustituible por llamada LLM en Sprint 3 sin cambiar la firma ni el contrato.
     */
    private String buildResumenAutomatico(User competitor,
                                           List<AreaMejoraDto> areas,
                                           Long categoryId) {
        int totalComentarios = areas.stream()
                .mapToInt(a -> a.getComentarios().size())
                .sum();

        if (totalComentarios == 0) {
            return "Aún no hay comentarios de expertos registrados para " + competitor.getName()
                    + (categoryId != null ? " en esta categoría." : ".")
                    + " Una vez los expertos evalúen el proyecto, aquí aparecerá el análisis detallado.";
        }

        // Aperturas variadas por criterio para evitar repetición mecánica
        String[] aperturas = {
                "En el ámbito de",
                "Respecto a",
                "En cuanto a",
                "Sobre el criterio de",
                "En relación con"
        };

        // Cierres contextuales según número de criterios con feedback
        String[] cierres = {
                "Se recomienda priorizar las áreas con mayor coincidencia entre evaluadores y trabajar iterativamente sobre los puntos señalados.",
                "El siguiente paso es revisar cada criterio en detalle y definir acciones concretas de mejora antes de la siguiente iteración.",
                "Con este feedback como base, se pueden establecer objetivos claros de mejora para la próxima fase del proyecto.",
                "Se sugiere compartir este análisis con el equipo y asignar responsables para cada área de mejora identificada."
        };

        StringBuilder sb = new StringBuilder();

        // Párrafo de apertura personalizado
        Set<String> todosEvaluadores = new LinkedHashSet<>();
        for (AreaMejoraDto area : areas) {
            for (ComentarioExpertoDto c : area.getComentarios()) {
                if (c.getEvaluadorNombre() != null && !c.getEvaluadorNombre().isBlank()) {
                    todosEvaluadores.add(c.getEvaluadorNombre());
                }
            }
        }
        sb.append("A continuación se recoge el análisis del feedback recibido por ")
          .append(competitor.getName())
          .append(" a partir de ")
          .append(totalComentarios == 1 ? "1 comentario" : totalComentarios + " comentarios")
          .append(" de experto")
          .append(totalComentarios == 1 ? "" : "s");
        if (!todosEvaluadores.isEmpty()) {
            sb.append(" (").append(String.join(", ", todosEvaluadores)).append(")");
        }
        sb.append(".\n\n");

        // Un párrafo por criterio con citas reales
        int apertura = 0;
        int criteriosConFeedback = 0;
        for (AreaMejoraDto area : areas) {
            List<ComentarioExpertoDto> comentarios = area.getComentarios();
            if (comentarios.isEmpty()) continue;
            criteriosConFeedback++;

            sb.append(aperturas[apertura % aperturas.length])
              .append(" **").append(area.getCriterioNombre()).append("**");
            apertura++;

            if (comentarios.size() == 1) {
                ComentarioExpertoDto c = comentarios.get(0);
                sb.append(", ").append(nombreOExperto(c.getEvaluadorNombre()))
                  .append(" señala: \"").append(truncar(c.getTexto(), 130)).append("\".");
            } else {
                // Varios comentarios: citar el primero y el último como representativos
                ComentarioExpertoDto primero = comentarios.get(0);
                ComentarioExpertoDto ultimo  = comentarios.get(comentarios.size() - 1);
                sb.append(", los evaluadores aportan perspectivas complementarias. ")
                  .append(nombreOExperto(primero.getEvaluadorNombre()))
                  .append(" apunta: \"").append(truncar(primero.getTexto(), 110)).append("\"");
                if (!primero.getEvaluadorNombre().equals(ultimo.getEvaluadorNombre())
                        || !primero.getTexto().equals(ultimo.getTexto())) {
                    sb.append("; ")
                      .append(nombreOExperto(ultimo.getEvaluadorNombre()))
                      .append(" añade: \"").append(truncar(ultimo.getTexto(), 110)).append("\"");
                }
                sb.append(".");
            }
            sb.append("\n\n");
        }

        // Cierre contextual rotativo según número de criterios
        sb.append(cierres[criteriosConFeedback % cierres.length]);

        return sb.toString();
    }

    /** Devuelve el nombre del evaluador o "el experto" si está vacío. */
    private String nombreOExperto(String nombre) {
        return (nombre != null && !nombre.isBlank()) ? nombre : "el experto";
    }

    /** Trunca en el último espacio antes de maxLen para no cortar a mitad de palabra. */
    private String truncar(String texto, int maxLen) {
        if (texto == null) return "";
        texto = texto.trim();
        if (texto.length() <= maxLen) return texto;
        int corte = texto.lastIndexOf(' ', maxLen);
        return (corte > maxLen / 2 ? texto.substring(0, corte) : texto.substring(0, maxLen)) + "…";
    }

    /**
     * Extrae el campo "texto" del JSON almacenado en datos.
     * Formato esperado: {"texto": "contenido del comentario"}
     * Usa parsing manual para evitar dependencias adicionales.
     */
    private String extraerTexto(String datos) {
        if (datos == null || datos.isBlank()) return "";
        // Buscar el valor del campo "texto"
        int idx = datos.indexOf("\"texto\"");
        if (idx == -1) return datos.trim(); // fallback: devolver el campo entero
        int colon = datos.indexOf(':', idx);
        if (colon == -1) return datos.trim();
        int quoteOpen = datos.indexOf('"', colon + 1);
        if (quoteOpen == -1) return datos.trim();
        int quoteClose = datos.indexOf('"', quoteOpen + 1);
        if (quoteClose == -1) return datos.trim();
        return datos.substring(quoteOpen + 1, quoteClose);
    }

    private Optional<HojaRutaMejora> findExisting(Long competitorId, Long categoryId) {
        if (categoryId != null) {
            return hojaRutaRepository.findByCompetitorIdAndCategoryId(competitorId, categoryId);
        }
        return hojaRutaRepository.findByCompetitorIdAndCategoryIsNull(competitorId);
    }

    private void deleteExisting(Long competitorId, Long categoryId) {
        if (categoryId != null) {
            hojaRutaRepository.deleteByCompetitorIdAndCategoryId(competitorId, categoryId);
        } else {
            hojaRutaRepository.deleteByCompetitorIdAndCategoryIsNull(competitorId);
        }
    }

    private HojaRutaMejoraDto toDto(HojaRutaMejora entidad, List<AreaMejoraDto> areas) {
        Long categoryId = entidad.getCategory() != null ? entidad.getCategory().getId() : null;
        return new HojaRutaMejoraDto(
                entidad.getId(),
                entidad.getCompetitor().getId(),
                categoryId,
                entidad.getResumenGenerado(),
                areas,
                entidad.isGeneradoIa(),
                entidad.getFechaGeneracion()
        );
    }
}
