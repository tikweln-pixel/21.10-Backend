package com.votify.controller;

import com.votify.dto.OrganizerDto;
import com.votify.service.OrganizerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizers")
public class OrganizerController {

    private final OrganizerService organizerService;

    public OrganizerController(OrganizerService organizerService) {
        this.organizerService = organizerService;
    }

    @GetMapping
    public ResponseEntity<List<OrganizerDto>> getAll() {
        return ResponseEntity.ok(organizerService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganizerDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(organizerService.findById(id));
    }

    @PostMapping
    public ResponseEntity<OrganizerDto> create(@RequestBody OrganizerDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(organizerService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrganizerDto> update(@PathVariable Long id, @RequestBody OrganizerDto dto) {
        return ResponseEntity.ok(organizerService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        organizerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
