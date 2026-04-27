package com.votify.controller;

import com.votify.dto.*;
import com.votify.service.CategoryService;
import com.votify.service.EventJuryService;
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
    private final EventJuryService eventJuryService;

    public EventController(EventService eventService,
                           EventParticipationService eventParticipationService,
                           CategoryService categoryService,
                           EventJuryService eventJuryService) {
        this.eventService = eventService;
        this.eventParticipationService = eventParticipationService;
        this.categoryService = categoryService;
        this.eventJuryService = eventJuryService;
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
    public ResponseEntity<Void> delete(@PathVariable Long id, @RequestParam Long userId) {
        eventService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    // ── Categories ────────────────────────────────────────────────────────────

    @GetMapping("/{eventId}/categories")
    public ResponseEntity<List<CategoryDto>> getCategories(@PathVariable Long eventId) {
        return ResponseEntity.ok(categoryService.findByEventId(eventId));
    }

    @PostMapping("/{eventId}/categories")
    public ResponseEntity<CategoryDto> addCategory(@PathVariable Long eventId, @RequestBody CategoryDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createForEvent(eventId, dto));
    }

    // ── Participations ────────────────────────────────────────────────────────

    @PostMapping("/{eventId}/participations")
    public ResponseEntity<EventParticipationDto> registerParticipation(
            @PathVariable Long eventId,
            @RequestBody RegisterParticipationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventParticipationService.registerParticipation(
                        eventId, request.getUserId(), request.getCategoryId(), request.getRole()));
    }

    @GetMapping("/{eventId}/users")
    public ResponseEntity<List<UserDto>> getUsersByEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventParticipationService.getUsersByEvent(eventId));
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

    @GetMapping("/{eventId}/categories/{categoryId}/spectators")
    public ResponseEntity<List<EventParticipationDto>> getSpectators(
            @PathVariable Long eventId, @PathVariable Long categoryId) {
        return ResponseEntity.ok(eventParticipationService.getSpectatorsByEventAndCategory(eventId, categoryId));
    }

    @PostMapping("/{eventId}/competitors")
    public ResponseEntity<EventParticipationDto> registerCompetitor(
            @PathVariable Long eventId,
            @RequestBody RegisterCompetitorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventParticipationService.registerCompetitor(eventId, request.getUserId(), request.getCategoryId()));
    }

    @PostMapping("/{eventId}/spectators/all-categories")
    public ResponseEntity<List<EventParticipationDto>> registerSpectatorInAllCategories(
            @PathVariable Long eventId,
            @RequestBody RegisterEventUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventParticipationService.ensureSpectatorRegistrationInAllCategories(eventId, request.getUserId()));
    }

    @PatchMapping("/{eventId}/users/{userId}/categories/{categoryId}/role")
    public ResponseEntity<EventParticipationDto> changeRole(
            @PathVariable Long eventId,
            @PathVariable Long userId,
            @PathVariable Long categoryId,
            @RequestBody ChangeRoleRequest request) {
        return ResponseEntity.ok(
                eventParticipationService.changeRole(eventId, userId, categoryId, request.getRole()));
    }

    @DeleteMapping("/{eventId}/participations")
    public ResponseEntity<Void> removeParticipation(
            @PathVariable Long eventId,
            @RequestParam Long userId,
            @RequestParam Long categoryId) {
        eventParticipationService.removeParticipation(eventId, userId, categoryId);
        return ResponseEntity.noContent().build();
    }

    // ── Roles summary (for UI) ────────────────────────────────────────────────

    @GetMapping("/users/{userId}/joined-event-ids")
    public ResponseEntity<List<Long>> getJoinedEventIds(@PathVariable Long userId) {
        return ResponseEntity.ok(eventParticipationService.getEventIdsWithParticipation(userId));
    }

    @GetMapping("/{eventId}/users/{userId}/has-participation")
    public ResponseEntity<java.util.Map<String, Boolean>> hasParticipation(
            @PathVariable Long eventId,
            @PathVariable Long userId) {
        boolean result = eventParticipationService.hasParticipationInEvent(eventId, userId);
        return ResponseEntity.ok(java.util.Map.of("hasParticipation", result));
    }

    @GetMapping("/{eventId}/users/{userId}/roles")
    public ResponseEntity<UserEventRolesDto> getUserRoles(
            @PathVariable Long eventId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(eventParticipationService.getUserRolesInEvent(eventId, userId));
    }

    // ── Jury ─────────────────────────────────────────────────────────────────

    @GetMapping("/{eventId}/jury")
    public ResponseEntity<List<EventJuryDto>> getJury(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventJuryService.getJuryByEvent(eventId));
    }

    @PostMapping("/{eventId}/jury")
    public ResponseEntity<EventJuryDto> registerJury(
            @PathVariable Long eventId,
            @RequestBody RegisterCompetitorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventJuryService.registerJury(eventId, request.getUserId()));
    }

    @DeleteMapping("/{eventId}/jury/{userId}")
    public ResponseEntity<Void> removeJury(
            @PathVariable Long eventId,
            @PathVariable Long userId) {
        eventJuryService.removeJury(eventId, userId);
        return ResponseEntity.noContent().build();
    }
}
