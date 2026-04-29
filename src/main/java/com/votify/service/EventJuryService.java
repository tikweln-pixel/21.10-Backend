package com.votify.service;

import com.votify.dto.EventJuryDto;
import com.votify.entity.Event;
import com.votify.entity.EventJury;
import com.votify.entity.User;
import com.votify.persistence.EventJuryRepository;
import com.votify.persistence.EventRepository;
import com.votify.persistence.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EventJuryService {

    private final EventJuryRepository eventJuryRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventParticipationService eventParticipationService;

    public EventJuryService(EventJuryRepository eventJuryRepository,
                             EventRepository eventRepository,
                             UserRepository userRepository,
                             EventParticipationService eventParticipationService) {
        this.eventJuryRepository = eventJuryRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.eventParticipationService = eventParticipationService;
    }

    public EventJuryDto registerJury(Long eventId, Long userId) {
        return registerJury(eventId, userId, null);
    }

    public EventJuryDto registerJury(Long eventId, Long userId, Long organizerUserId) {
        if (organizerUserId != null) {
            eventParticipationService.ensureUserHasOrganizerRole(eventId, organizerUserId);
        }
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con id: " + eventId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + userId));

        if (eventJuryRepository.existsByEventIdAndUserId(eventId, userId)) {
            throw new RuntimeException("El usuario " + userId + " ya es jurado del evento " + eventId);
        }

        EventJury jury = eventJuryRepository.save(new EventJury(event, user));
        return toDto(jury);
    }

    public List<EventJuryDto> getJuryByEvent(Long eventId) {
        List<EventJuryDto> result = new ArrayList<>();
        for (EventJury j : eventJuryRepository.findByEventId(eventId)) {
            result.add(toDto(j));
        }
        return result;
    }

    public void removeJury(Long eventId, Long userId) {
        removeJury(eventId, userId, null);
    }

    public void removeJury(Long eventId, Long userId, Long organizerUserId) {
        if (organizerUserId != null) {
            eventParticipationService.ensureUserHasOrganizerRole(eventId, organizerUserId);
        }
        EventJury jury = eventJuryRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new RuntimeException(
                        "El usuario " + userId + " no es jurado del evento " + eventId));
        eventJuryRepository.delete(jury);
    }

    private EventJuryDto toDto(EventJury jury) {
        return new EventJuryDto(
                jury.getId(),
                jury.getEvent().getId(),
                jury.getUser().getId(),
                jury.getUser().getName(),
                jury.getUser().getEmail()
        );
    }
}
