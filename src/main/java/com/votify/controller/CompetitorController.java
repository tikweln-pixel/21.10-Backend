package com.votify.controller;

import com.votify.advice.ForbiddenException;
import com.votify.dto.CompetitorCommentDto;
import com.votify.dto.CompetitorDto;
import com.votify.dto.HojaRutaMejoraDto;
import com.votify.dto.AreaMejoraDto;
import com.votify.dto.ComentarioExpertoDto;
import com.votify.entity.Comment;
import com.votify.entity.Project;
import com.votify.entity.User;
import com.votify.persistence.CommentRepository;
import com.votify.persistence.EventRepository;
import com.votify.persistence.ProjectRepository;
import com.votify.persistence.UserRepository;
import com.votify.service.HojaRutaMejoraService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/competitors")
public class CompetitorController {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final CommentRepository commentRepository;
    private final HojaRutaMejoraService hojaRutaService;
    private final EventRepository eventRepository;

    public CompetitorController(UserRepository userRepository,
                                ProjectRepository projectRepository,
                                CommentRepository commentRepository,
                                HojaRutaMejoraService hojaRutaService,
                                EventRepository eventRepository) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.commentRepository = commentRepository;
        this.hojaRutaService = hojaRutaService;
        this.eventRepository = eventRepository;
    }

    /**
     * Valida que el solicitante (requesterId) puede acceder a los datos del competidor.
     * Permite acceso si: el solicitante ES el competidor, o es organizador de algún evento.
     * Si requesterId es null (header ausente), se permite por compatibilidad con herramientas de admin.
     */
    private void checkAccess(Long requesterId, Long competitorId) {
        if (requesterId == null) return;
        if (requesterId.equals(competitorId)) return;
        if (eventRepository.existsByOrganizerId(requesterId)) return;
        throw new ForbiddenException("Acceso denegado: no puedes ver datos de otro competidor");
    }

    @GetMapping
    public ResponseEntity<List<CompetitorDto>> getAll() {
        List<CompetitorDto> result = new ArrayList<>();
        for (User user : userRepository.findAll()) {
            result.add(new CompetitorDto(user.getId(), user.getName(), user.getEmail()));
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompetitorDto> getById(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return ResponseEntity.ok(new CompetitorDto(user.getId(), user.getName(), user.getEmail()));
    }

    @GetMapping("/{competitorId}/comments")
    public ResponseEntity<List<CompetitorCommentDto>> getCommentsByCompetitor(
            @PathVariable Long competitorId,
            @RequestHeader(value = "X-User-Id", required = false) Long requesterId) {
        checkAccess(requesterId, competitorId);
        List<Project> projects = new ArrayList<>();
        for (Project p : projectRepository.findAll()) {
            for (User c : p.getCompetitors()) {
                if (c.getId().equals(competitorId)) {
                    projects.add(p);
                    break;
                }
            }
        }

        List<Long> projectIds = new ArrayList<>();
        for (Project p : projects) {
            projectIds.add(p.getId());
        }

        if (projectIds.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<CompetitorCommentDto> comments = new ArrayList<>();
        for (Comment c : commentRepository.findByProjectIdIn(projectIds)) {
            Long voterId = c.getVoter() != null ? c.getVoter().getId() : null;
            String voterName = c.getVoter() != null ? c.getVoter().getName() : null;
            comments.add(new CompetitorCommentDto(
                    c.getId(), c.getText(), voterId, voterName,
                    c.getProject().getId(), c.getProject().getName()));
        }

        return ResponseEntity.ok(comments);
    }

    /**
     * Recupera la hoja de ruta de mejora del competidor.
     * Si no existe, la genera automáticamente a partir de sus EvaluacionComentario.
     *
     * @param competitorId ID del competidor
     * @param categoryId   (opcional) filtrar por categoría
     */
    @GetMapping("/{competitorId}/hoja-ruta")
    public ResponseEntity<HojaRutaMejoraDto> getHojaRuta(
            @PathVariable Long competitorId,
            @RequestParam(required = false) Long categoryId,
            @RequestHeader(value = "X-User-Id", required = false) Long requesterId) {
        checkAccess(requesterId, competitorId);
        HojaRutaMejoraDto dto = hojaRutaService.getOrGenerar(competitorId, categoryId);
        return ResponseEntity.ok(dto);
    }

    /**
     * Regenera la hoja de ruta del competidor desde cero,
     * sobreescribiendo la versión anterior.
     *
     * @param competitorId ID del competidor
     * @param categoryId   (opcional) filtrar por categoría
     */
    @PostMapping("/{competitorId}/hoja-ruta/generar")
    public ResponseEntity<HojaRutaMejoraDto> generarHojaRuta(
            @PathVariable Long competitorId,
            @RequestParam(required = false) Long categoryId,
            @RequestHeader(value = "X-User-Id", required = false) Long requesterId) {
        checkAccess(requesterId, competitorId);
        HojaRutaMejoraDto dto = hojaRutaService.generar(competitorId, categoryId);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * Descarga la hoja de ruta del competidor como fichero de texto plano.
     * Si no existe, la genera automáticamente antes de devolverla.
     *
     * @param competitorId ID del competidor
     * @param categoryId   (opcional) filtrar por categoría
     */
    @GetMapping("/{competitorId}/hoja-ruta/pdf")
    public ResponseEntity<byte[]> descargarHojaRuta(
            @PathVariable Long competitorId,
            @RequestParam(required = false) Long categoryId,
            @RequestHeader(value = "X-User-Id", required = false) Long requesterId) {
        checkAccess(requesterId, competitorId);
        HojaRutaMejoraDto dto = hojaRutaService.getOrGenerar(competitorId, categoryId);
        byte[] content = buildTextoDescargable(dto).getBytes(StandardCharsets.UTF_8);

        String filename = "hoja-ruta-competidor-" + competitorId
                + (categoryId != null ? "-cat" + categoryId : "") + ".txt";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(content);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Métodos privados
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Formatea la hoja de ruta como texto plano estructurado para descarga.
     */
    private String buildTextoDescargable(HojaRutaMejoraDto dto) {
        StringBuilder sb = new StringBuilder();
        String fecha = dto.getFechaGeneracion() != null
                ? new SimpleDateFormat("dd/MM/yyyy HH:mm").format(dto.getFechaGeneracion())
                : "—";

        sb.append("═══════════════════════════════════════════════════════\n");
        sb.append("          HOJA DE RUTA DE MEJORA — VOTIFY\n");
        sb.append("═══════════════════════════════════════════════════════\n\n");
        sb.append("Competidor ID : ").append(dto.getCompetitorId()).append("\n");
        if (dto.getCategoryId() != null) {
            sb.append("Categoría ID  : ").append(dto.getCategoryId()).append("\n");
        }
        sb.append("Generado      : ").append(fecha).append("\n");
        sb.append("Modo          : ").append(dto.isGeneradoIa() ? "IA generativa" : "Estructurado automático").append("\n\n");

        sb.append("───────────────────────────────────────────────────────\n");
        sb.append("RESUMEN GENERAL\n");
        sb.append("───────────────────────────────────────────────────────\n");
        sb.append(dto.getResumenGeneral()).append("\n\n");

        if (dto.getAreasMejora() != null && !dto.getAreasMejora().isEmpty()) {
            sb.append("───────────────────────────────────────────────────────\n");
            sb.append("ÁREAS DE MEJORA POR CRITERIO\n");
            sb.append("───────────────────────────────────────────────────────\n\n");
            for (AreaMejoraDto area : dto.getAreasMejora()) {
                sb.append("▶ ").append(area.getCriterioNombre()).append("\n");
                if (area.getComentarios() != null) {
                    for (ComentarioExpertoDto c : area.getComentarios()) {
                        sb.append("  • [").append(c.getEvaluadorNombre()).append("] ")
                          .append(c.getTexto()).append("\n");
                    }
                }
                sb.append("\n");
            }
        } else {
            sb.append("Sin comentarios de expertos registrados.\n\n");
        }

        sb.append("═══════════════════════════════════════════════════════\n");
        sb.append("Generado por Votify — sistema de votaciones y competiciones\n");
        return sb.toString();
    }
}

