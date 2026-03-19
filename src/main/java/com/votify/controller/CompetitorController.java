package com.votify.controller;

import com.votify.dto.CompetitorDto;
import com.votify.service.CompetitorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/competitors")
public class CompetitorController {

    private final CompetitorService competitorService;

    public CompetitorController(CompetitorService competitorService) {
        this.competitorService = competitorService;
    }

    @GetMapping
    public ResponseEntity<List<CompetitorDto>> getAll() {
        return ResponseEntity.ok(competitorService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompetitorDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(competitorService.findById(id));
    }

    @PostMapping
    public ResponseEntity<CompetitorDto> create(@RequestBody CompetitorDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(competitorService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompetitorDto> update(@PathVariable Long id, @RequestBody CompetitorDto dto) {
        return ResponseEntity.ok(competitorService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        competitorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
