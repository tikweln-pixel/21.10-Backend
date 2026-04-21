package com.votify.controller;

import com.votify.dto.*;
import com.votify.service.CategoryService;
import com.votify.service.EventParticipationService;
import com.votify.service.EventService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;
    private final EventParticipationService eventParticipationService;
    private final CategoryService categoryService;

    public EventController(EventService eventService, EventParticipationService eventParticipationService, CategoryService categoryService) {
        this.eventService = eventService;
        this.eventParticipationService = eventParticipationService;
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<List<EventDto>> getAll() {
        return ResponseEntity.ok(eventService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.findById(id));
    }

    @PostMapping
    public ResponseEntity<EventDto> create(@RequestBody EventDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.create(request));
    }

    @PostMapping("/by-organizer")
    public ResponseEntity<EventDto> createByOrganizer(@RequestBody EventDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.createForOrganizer(dto.getOrganizerId(), dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventDto> update(@PathVariable Long id, @RequestBody EventDto dto) {
        return ResponseEntity.ok(eventService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        eventService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{eventId}/categories")
    public ResponseEntity<List<CategoryDto>> getCategories(@PathVariable Long eventId) {
        return ResponseEntity.ok(categoryService.findByEventId(eventId));
    }

    @PostMapping("/{eventId}/categories")
    public ResponseEntity<CategoryDto> addCategory(@PathVariable Long eventId, @RequestBody CategoryDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createForEvent(eventId, dto));
    }

    @PostMapping("/{eventId}/participations")
    public ResponseEntity<EventParticipationDto> registerParticipation(
            @PathVariable Long eventId,
            @RequestBody RegisterParticipationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventParticipationService.registerParticipation(
                        eventId, request.getUserId(), request.getCategoryId(), request.getRole()));
    }

    @GetMapping("/{eventId}/participations")
    public ResponseEntity<List<EventParticipationDto>> getParticipations(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventParticipationService.getParticipationsByEvent(eventId));
    }

    @GetMapping("/{eventId}/categories/{categoryId}/participations")
    public ResponseEntity<List<EventParticipationDto>> getParticipationsByCategory(
            @PathVariable Long eventId, @PathVariable Long categoryId) {
        return ResponseEntity.ok(eventParticipationService.getParticipationsByEventAndCategory(eventId, categoryId));
    }

    @GetMapping("/{eventId}/categories/{categoryId}/competitors")
    public ResponseEntity<List<EventParticipationDto>> getCompetitors(
            @PathVariable Long eventId, @PathVariable Long categoryId) {
        return ResponseEntity.ok(eventParticipationService.getCompetitorsByEventAndCategory(eventId, categoryId));
    }

    @GetMapping("/{eventId}/categories/{categoryId}/voters")
    public ResponseEntity<List<EventParticipationDto>> getVoters(
            @PathVariable Long eventId, @PathVariable Long categoryId) {
        return ResponseEntity.ok(eventParticipationService.getVotersByEventAndCategory(eventId, categoryId));
    }

    @PostMapping("/{eventId}/competitors")
    public ResponseEntity<EventParticipationDto> registerCompetitor(
            @PathVariable Long eventId,
            @RequestBody RegisterCompetitorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventParticipationService.registerCompetitor(eventId, request.getUserId(), request.getCategoryId()));
    }

    @PostMapping("/{eventId}/voters")
    public ResponseEntity<EventParticipationDto> registerVoter(
            @PathVariable Long eventId,
            @RequestBody RegisterCompetitorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventParticipationService.registerVoter(eventId, request.getUserId(), request.getCategoryId()));
    }

    @DeleteMapping("/{eventId}/participations")
    public ResponseEntity<Void> removeParticipation(
            @PathVariable Long eventId,
            @RequestParam Long userId,
            @RequestParam Long categoryId) {
        eventParticipationService.removeParticipation(eventId, userId, categoryId);
        return ResponseEntity.noContent().build();
    }

}
