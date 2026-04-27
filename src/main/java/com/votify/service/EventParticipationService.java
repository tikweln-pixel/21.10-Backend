package com.votify.service;

import com.votify.dto.EventParticipationDto;
import com.votify.dto.UserDto;
import com.votify.dto.UserEventRolesDto;
import com.votify.entity.*;
import com.votify.persistence.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Service
public class EventParticipationService {

    private final EventParticipationRepository eventParticipationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EventJuryRepository eventJuryRepository;

    public EventParticipationService(EventParticipationRepository eventParticipationRepository,
                                     EventRepository eventRepository,
                                     UserRepository userRepository,
                                     CategoryRepository categoryRepository,
                                     EventJuryRepository eventJuryRepository) {
        this.eventParticipationRepository = eventParticipationRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.eventJuryRepository = eventJuryRepository;
    }

    public EventParticipationDto registerParticipation(Long eventId, Long userId, Long categoryId, ParticipationRole role) {
        if (eventId == null) throw new RuntimeException("El ID del evento es obligatorio");
        if (userId == null) throw new RuntimeException("El ID del usuario es obligatorio");
        if (categoryId == null) throw new RuntimeException("La categoría es obligatoria para la participación");

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con id: " + eventId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + userId));
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con id: " + categoryId));

        if (!category.getEvent().getId().equals(eventId)) {
            throw new RuntimeException("La categoría no pertenece a este evento");
        }

        if (eventParticipationRepository.existsByEventIdAndUserIdAndCategoryId(eventId, userId, categoryId)) {
            throw new RuntimeException("El usuario " + userId + " ya está registrado en la categoría " + categoryId + " del evento " + eventId);
        }

        EventParticipation participation = new EventParticipation(event, user, category, role);
        return toDto(eventParticipationRepository.save(Objects.requireNonNull(participation)));
    }

    public EventParticipationDto registerCompetitor(Long eventId, Long userId, Long categoryId) {
        return ensureCompetitorRegistration(eventId, userId, categoryId);
    }


    public EventParticipationDto ensureCompetitorRegistration(Long eventId, Long userId, Long categoryId) {
        if (eventId == null) throw new RuntimeException("El ID del evento es obligatorio");
        if (userId == null) throw new RuntimeException("El ID del usuario es obligatorio");
        if (categoryId == null) throw new RuntimeException("La categoría es obligatoria para la participación");

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con id: " + eventId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + userId));
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con id: " + categoryId));

        if (!category.getEvent().getId().equals(eventId)) {
            throw new RuntimeException("La categoría no pertenece a este evento");
        }

        EventParticipation participation = eventParticipationRepository
                .findByEventIdAndUserIdAndCategoryId(eventId, userId, categoryId)
                .orElseGet(() -> new EventParticipation(event, user, category, ParticipationRole.COMPETITOR));

        participation.setRole(ParticipationRole.COMPETITOR);
        EventParticipation saved = eventParticipationRepository.save(participation);
        autoRegisterSpectatorInOtherCategories(eventId, userId, categoryId);
        return toDto(saved);
    }

    //Permite registrar a un usuario como espectador en todas las categorías de un evento
    public List<EventParticipationDto> ensureSpectatorRegistrationInAllCategories(Long eventId, Long userId) {
        if (eventId == null) throw new RuntimeException("El ID del evento es obligatorio");
        if (userId == null) throw new RuntimeException("El ID del usuario es obligatorio");

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con id: " + eventId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + userId));

        List<Category> allCategories = categoryRepository.findByEventId(eventId);
        if (allCategories.isEmpty()) {
            throw new RuntimeException("El evento no tiene categorias para registrar al usuario");
        }

        List<EventParticipationDto> result = new ArrayList<>();
        for (Category category : allCategories) {
            java.util.Optional<EventParticipation> existing = eventParticipationRepository
                    .findByEventIdAndUserIdAndCategoryId(eventId, userId, category.getId());

            if (existing.isPresent()) {
                result.add(toDto(existing.get()));
            } else {
                EventParticipation participation = new EventParticipation(event, user, category, ParticipationRole.SPECTATOR);
                result.add(toDto(eventParticipationRepository.save(participation)));
            }
        }
        return result;
    }

    private void autoRegisterSpectatorInOtherCategories(Long eventId, Long userId, Long excludedCategoryId) {
        Event event = eventRepository.findById(eventId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();
        List<Category> allCategories = categoryRepository.findByEventId(eventId);
        for (Category cat : allCategories) {
            if (!cat.getId().equals(excludedCategoryId)
                    && !eventParticipationRepository.existsByEventIdAndUserIdAndCategoryId(eventId, userId, cat.getId())) {
                eventParticipationRepository.save(new EventParticipation(event, user, cat, ParticipationRole.SPECTATOR));
            }
        }
    }

    public EventParticipationDto changeRole(Long eventId, Long userId, Long categoryId, ParticipationRole newRole) {
        EventParticipation participation = eventParticipationRepository
                .findByEventIdAndUserIdAndCategoryId(eventId, userId, categoryId)
                .orElseThrow(() -> new RuntimeException(
                        "Participación no encontrada para usuario " + userId + " en categoría " + categoryId));

        if (newRole == ParticipationRole.COMPETITOR && participation.getRole() != ParticipationRole.COMPETITOR) {
            
        }
        participation.setRole(newRole);
        return toDto(eventParticipationRepository.save(participation));
    }

    public UserEventRolesDto getUserRolesInEvent(Long eventId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + userId));

        boolean isJury = eventJuryRepository.existsByEventIdAndUserId(eventId, userId);

        List<Category> allCategories = categoryRepository.findByEventId(eventId);
        List<EventParticipation> participations = eventParticipationRepository
                .findByEventIdAndUserId(eventId, userId);

        List<UserEventRolesDto.CategoryRoleDto> categoryRoles = new ArrayList<>();
        for (Category cat : allCategories) {
            String role = "SPECTATOR";
            for (EventParticipation p : participations) {
                if (p.getCategory().getId().equals(cat.getId())) {
                    role = p.getRole().name();
                    break;
                }
            }
            categoryRoles.add(new UserEventRolesDto.CategoryRoleDto(cat.getId(), cat.getName(), role));
        }

        String primaryRole = computePrimaryRole(isJury, categoryRoles);

        return new UserEventRolesDto(userId, user.getName(), eventId, isJury, primaryRole, categoryRoles);
    }

    private String computePrimaryRole(boolean isJury, List<UserEventRolesDto.CategoryRoleDto> categoryRoles) {
        if (isJury) return "JURY";
        for (UserEventRolesDto.CategoryRoleDto cr : categoryRoles) {
            if ("COMPETITOR".equals(cr.getRole())) return "COMPETITOR";
        }
        return "SPECTATOR";
    }

    //Devuelve lista de Usuarios que participan en un evento
    public List<UserDto> getUsersByEvent(Long eventId) {
        List<EventParticipation> participations = eventParticipationRepository.findByEventId(eventId);
        List<UserDto> result = new ArrayList<>();
        for (EventParticipation p : participations) {
            boolean alreadyAdded = false;
            for (UserDto u : result) {
                if (u.getId().equals(p.getUser().getId())) {
                    alreadyAdded = true;
                    break;
                }
            }
            if (!alreadyAdded) {
                result.add(new UserDto(p.getUser().getId(), p.getUser().getName(), p.getUser().getEmail()));
            }
        }
        return result;
    }

    public List<EventParticipationDto> getParticipationsByEvent(Long eventId) {
        List<EventParticipation> participations = eventParticipationRepository.findByEventId(eventId);
        List<EventParticipationDto> result = new ArrayList<>();
        for (EventParticipation p : participations) {
            result.add(toDto(p));
        }
        return result;
    }

    public List<EventParticipationDto> getParticipationsByEventAndCategory(Long eventId, Long categoryId) {
        List<EventParticipation> participations = eventParticipationRepository.findByEventIdAndCategoryId(eventId, categoryId);
        List<EventParticipationDto> result = new ArrayList<>();
        for (EventParticipation p : participations) {
            result.add(toDto(p));
        }
        return result;
    }

    public List<EventParticipationDto> getCompetitorsByEventAndCategory(Long eventId, Long categoryId) {
        List<EventParticipation> participations = eventParticipationRepository
                .findByEventIdAndCategoryIdAndRole(eventId, categoryId, ParticipationRole.COMPETITOR);
        List<EventParticipationDto> result = new ArrayList<>();
        for (EventParticipation p : participations) {
            result.add(toDto(p));
        }
        return result;
    }

    public List<EventParticipationDto> getSpectatorsByEventAndCategory(Long eventId, Long categoryId) {
        List<EventParticipation> participations = eventParticipationRepository
                .findByEventIdAndCategoryIdAndRole(eventId, categoryId, ParticipationRole.SPECTATOR);
        List<EventParticipationDto> result = new ArrayList<>();
        for (EventParticipation p : participations) {
            result.add(toDto(p));
        }
        return result;
    }

    public List<EventParticipationDto> getParticipationsByUser(Long userId) {
        List<EventParticipation> participations = eventParticipationRepository.findByUserId(userId);
        List<EventParticipationDto> result = new ArrayList<>();
        for (EventParticipation p : participations) {
            result.add(toDto(p));
        }
        return result;
    }

    public boolean hasParticipationInEvent(Long eventId, Long userId) {
        return eventParticipationRepository.existsByEventIdAndUserId(eventId, userId);
    }

    //Devuelve na lista de IDs de eventos en los que el usuario tiene alguna participación
    public List<Long> getEventIdsWithParticipation(Long userId) {
        List<EventParticipation> participations = eventParticipationRepository.findByUserId(userId);
        List<Long> result = new ArrayList<>();
        for (EventParticipation p : participations) {
            Long eventId = p.getEvent().getId();
            boolean alreadyAdded = false;
            for (Long id : result) {
                if (id.equals(eventId)) {
                    alreadyAdded = true;
                    break;
                }
            }
            if (!alreadyAdded) {
                result.add(eventId);
            }
        }
        return result;
    }

    public void removeParticipation(Long eventId, Long userId, Long categoryId) {
        if (eventId == null || userId == null || categoryId == null) {
            throw new RuntimeException("Los IDs son obligatorios para eliminar la participación");
        }
        EventParticipation participation = eventParticipationRepository
                .findByEventIdAndUserIdAndCategoryId(eventId, userId, categoryId)
                .orElseThrow(() -> new RuntimeException("Participación no encontrada"));
        eventParticipationRepository.delete(participation);
    }

    private EventParticipationDto toDto(EventParticipation participation) {
        return new EventParticipationDto(
                participation.getId(),
                participation.getEvent().getId(),
                participation.getUser().getId(),
                participation.getCategory().getId(),
                participation.getRole(),
                participation.getUser().getName(),
                participation.getUser().getEmail(),
                participation.getCategory().getName()
        );
    }
}
