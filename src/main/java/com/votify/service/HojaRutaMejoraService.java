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
     * Genera el texto del resumen automático a partir de los datos disponibles.
     * En la implementación parcial no usa IA; simplemente describe el contenido.
     */
    private String buildResumenAutomatico(User competitor,
                                           List<AreaMejoraDto> areas,
                                           Long categoryId) {
        int totalComentarios = areas.stream()
                .mapToInt(a -> a.getComentarios().size())
                .sum();
        int totalCriterios = areas.size();

        if (totalComentarios == 0) {
            return "Aún no hay comentarios de expertos registrados para este competidor"
                    + (categoryId != null ? " en esta categoría." : ".");
        }

        Set<String> evaluadores = new LinkedHashSet<>();
        for (AreaMejoraDto area : areas) {
            for (ComentarioExpertoDto c : area.getComentarios()) {
                if (c.getEvaluadorNombre() != null) evaluadores.add(c.getEvaluadorNombre());
            }
        }

        return String.format(
                "Resumen generado automáticamente. %s ha recibido %d comentario%s de expertos "
                + "en %d criterio%s%s. Revisa las áreas de mejora para ver el detalle por criterio.",
                competitor.getName(),
                totalComentarios,
                totalComentarios == 1 ? "" : "s",
                totalCriterios,
                totalCriterios == 1 ? "" : "s",
                evaluadores.isEmpty() ? "" : " (evaluadores: " + String.join(", ", evaluadores) + ")"
        );
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
