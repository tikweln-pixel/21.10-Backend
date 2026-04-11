package com.votify.controller;

import com.votify.dto.EvaluacionDto;
import com.votify.service.EvaluacionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evaluaciones")
public class EvaluacionController {

    private final EvaluacionService evaluacionService;

    public EvaluacionController(EvaluacionService evaluacionService) {
        this.evaluacionService = evaluacionService;
    }

    @GetMapping
    public ResponseEntity<List<EvaluacionDto>> getAll() {
        return ResponseEntity.ok(evaluacionService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EvaluacionDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(evaluacionService.findById(id));
    }

    @PostMapping
    public ResponseEntity<EvaluacionDto> create(@RequestBody EvaluacionDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(evaluacionService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EvaluacionDto> update(@PathVariable Long id, @RequestBody EvaluacionDto dto) {
        return ResponseEntity.ok(evaluacionService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        evaluacionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-category")
    public ResponseEntity<List<EvaluacionDto>> getByCategory(@RequestParam Long categoryId) {
        return ResponseEntity.ok(evaluacionService.findByCategory(categoryId));
    }

    @GetMapping("/by-competitor")
    public ResponseEntity<List<EvaluacionDto>> getByCompetitor(@RequestParam Long competitorId) {
        return ResponseEntity.ok(evaluacionService.findByCompetitor(competitorId));
    }

    @GetMapping("/by-category-competitor")
    public ResponseEntity<List<EvaluacionDto>> getByCategoryAndCompetitor(
            @RequestParam Long categoryId, @RequestParam Long competitorId) {
        return ResponseEntity.ok(evaluacionService.findByCategoryAndCompetitor(categoryId, competitorId));
    }
}
