package com.votify.controller;

import com.votify.dto.CategoryCriterionPointsDto;
import com.votify.dto.CategoryDto;
import com.votify.dto.UserDto;
import com.votify.entity.VotingType;
import com.votify.service.CategoryService;
import com.votify.service.VotingService;
import com.votify.entity.User;
import com.votify.persistence.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import org.springframework.lang.NonNull;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final VotingService votingService;
    private final UserRepository userRepository;

    public CategoryController(CategoryService categoryService,
            VotingService votingService,
            UserRepository userRepository) {
        this.categoryService = categoryService;
        this.votingService = votingService;
        this.userRepository = userRepository;
    }

    // CRUD básico

    @GetMapping
    public ResponseEntity<List<CategoryDto>> getAll() {
        return ResponseEntity.ok(categoryService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryDto> getById(@PathVariable @NonNull Long id) {
        return ResponseEntity.ok(categoryService.findById(id));
    }

    @PostMapping
    public ResponseEntity<CategoryDto> create(@RequestBody CategoryDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryDto> update(@PathVariable Long id, @RequestBody CategoryDto dto) {
        return ResponseEntity.ok(categoryService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @RequestParam Long userId) {
        categoryService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    // Definir Categorías: tipo de votación

    // Asigna el tipo de votación a una categoría.

    // JURY_EXPERT → Votacion_Jurado_Exp (diagrama de clases)
    // POPULAR_VOTE → Voto_Popular (diagrama de clases)

    @PutMapping("/{id}/voting-type")
    public ResponseEntity<CategoryDto> setVotingType(
            @PathVariable Long id,
            @RequestParam VotingType type) {
        return ResponseEntity.ok(categoryService.setVotingType(id, type));
    }

    // Req. 4 – Configurar Puntos: puntos por criterio por categoría

    /**
     * Obtiene la configuración de puntos por criterio de una categoría
     */
    @GetMapping("/{id}/criterion-points")
    public ResponseEntity<List<CategoryCriterionPointsDto>> getCriterionPoints(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCriterionPoints(id));
    }

    /**
     * Actualiza (o crea) los puntos máximos de un criterio concreto en la
     * categoría.
     * El cuerpo lleva { "weightPercent": <valor> }.
     */
    @PutMapping("/{id}/criterion-points/{criterionId}")
    public ResponseEntity<CategoryCriterionPointsDto> setCriterionPoints(
            @PathVariable Long id,
            @PathVariable Long criterionId,
            @RequestBody CategoryCriterionPointsDto dto) {
        return ResponseEntity.ok(categoryService.setCriterionPoints(id, criterionId, dto.getWeightPercent()));
    }

    /**
     * Reemplaza toda la configuración de puntos de una categoría de golpe.
     * Usado cuando el organizador pulsa "Aceptar" en la pantalla de sliders.
     */
    @PutMapping("/{id}/criterion-points/bulk")
    public ResponseEntity<List<CategoryCriterionPointsDto>> setCriterionPointsBulk(
            @PathVariable Long id,
            @RequestBody List<CategoryCriterionPointsDto> dtos) {
        return ResponseEntity.ok(categoryService.setCriterionPointsBulk(id, dtos));
    }

    /**
     * Elimina la configuración de puntos de un criterio concreto en la categoría.
     */
    @DeleteMapping("/{id}/criterion-points/{criterionId}")
    public ResponseEntity<Void> deleteCriterionPoints(
            @PathVariable Long id,
            @PathVariable Long criterionId) {
        categoryService.deleteCriterionPoints(id, criterionId);
        return ResponseEntity.noContent().build();
    }

    // Req. 23 – Configurar Puntos POPULAR_VOTE

    /**
     * Obtiene el totalPoints configurado en una categoría POPULAR_VOTE.
     * GET /api/categories/{id}/total-points
     */
    @GetMapping("/{id}/total-points")
    public ResponseEntity<CategoryDto> getTotalPoints(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getTotalPoints(id));
    }

    /**
     * Configura el total de puntos a repartir en una categoría POPULAR_VOTE.
     * El cuerpo lleva { "totalPoints": <valor> }.
     * PUT /api/categories/{id}/total-points
     */
    @PutMapping("/{id}/total-points")
    public ResponseEntity<CategoryDto> setTotalPoints(
            @PathVariable Long id,
            @RequestBody CategoryDto dto) {
        return ResponseEntity.ok(categoryService.setTotalPoints(id, dto.getTotalPoints()));
    }

    // Req. 19 – Control de Voto POPULAR_VOTE: límite de competidores distintos

    /**
     * Configura el máximo de competidores distintos a los que puede votar un
     * votante.
     * Regla de negocio: de 5 proyectos se puede votar hasta 3 en una votación
     * popular.
     * El cuerpo lleva { "maxVotesPerVoter": <valor> }.
     * PUT /api/categories/{id}/max-votes-per-voter
     */
    @PutMapping("/{id}/max-votes-per-voter")
    public ResponseEntity<CategoryDto> setMaxVotesPerVoter(
            @PathVariable Long id,
            @RequestBody CategoryDto dto) {
        return ResponseEntity.ok(categoryService.setMaxVotesPerVoter(id, dto.getMaxVotesPerVoter()));
    }

    @GetMapping("/{categoryId}/active-voters")
    //
    public ResponseEntity<List<UserDto>> getActiveVoters(@PathVariable Long categoryId) {
        List<Long> voterIds = votingService.getActiveVoterIds(categoryId);
        if (voterIds == null || voterIds.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        List<UserDto> voters = new ArrayList<>();
        for (User u : userRepository.findAllById(voterIds)) {
            voters.add(new UserDto(u.getId(), u.getName(), u.getEmail()));
        }
        return ResponseEntity.ok(voters);
    }
}

