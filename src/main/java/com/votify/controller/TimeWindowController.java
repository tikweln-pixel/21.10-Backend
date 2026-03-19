package com.votify.controller;

import com.votify.dto.TimeWindowDto;
import com.votify.service.TimeWindowService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/time-windows")
public class TimeWindowController {

    private final TimeWindowService timeWindowService;

    public TimeWindowController(TimeWindowService timeWindowService) {
        this.timeWindowService = timeWindowService;
    }

    @GetMapping
    public ResponseEntity<List<TimeWindowDto>> getAll() {
        return ResponseEntity.ok(timeWindowService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TimeWindowDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(timeWindowService.findById(id));
    }

    @PostMapping
    public ResponseEntity<TimeWindowDto> create(@RequestBody TimeWindowDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(timeWindowService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TimeWindowDto> update(@PathVariable Long id, @RequestBody TimeWindowDto dto) {
        return ResponseEntity.ok(timeWindowService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        timeWindowService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
