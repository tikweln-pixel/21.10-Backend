package com.votify.controller;

import com.votify.dto.CriterionDto;
import com.votify.service.CriterionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/criteria")
public class CriterionController {

    private final CriterionService criterionService;

    public CriterionController(CriterionService criterionService) {
        this.criterionService = criterionService;
    }

    @GetMapping
    public ResponseEntity<List<CriterionDto>> getAll() {
        return ResponseEntity.ok(criterionService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CriterionDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(criterionService.findById(id));
    }

    @PostMapping
    public ResponseEntity<CriterionDto> create(@RequestBody CriterionDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(criterionService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CriterionDto> update(@PathVariable Long id, @RequestBody CriterionDto dto) {
        return ResponseEntity.ok(criterionService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        criterionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
