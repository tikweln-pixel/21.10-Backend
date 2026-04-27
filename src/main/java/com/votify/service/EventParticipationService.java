package com.votify.service;

import com.votify.dto.EventParticipationDto;
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

    public EventParticipationService(EventParticipationRepository eventParticipationRepository,
                                     EventRepository eventRepository,
                                     UserRepository userRepository,
                                     CategoryRepository categoryRepository) {
        this.eventParticipationRepository = eventParticipationRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
    }

    public EventParticipationDto registerParticipation(Long eventId, Long userId, Long categoryId, ParticipationRole role) {
        if (eventId == null) throw new RuntimeException("Event ID is required");
        if (userId == null) throw new RuntimeException("User ID is required");
        if (categoryId == null) throw new RuntimeException("Category is required for participation");

        Event event = eventRepository.findById(Objects.requireNonNull(eventId))
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));
        User user = userRepository.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        Category category = categoryRepository.findById(Objects.requireNonNull(categoryId))
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));

        if (!category.getEvent().getId().equals(eventId)) {
            throw new RuntimeException("Category does not belong to this event");
        }

        if (eventParticipationRepository.existsByEventIdAndUserIdAndCategoryId(
                Objects.requireNonNull(eventId), Objects.requireNonNull(userId), Objects.requireNonNull(categoryId))) {
            throw new RuntimeException("User " + userId + " is already registered in category " + categoryId + " of event " + eventId);
        }

        EventParticipation participation = new EventParticipation(
                Objects.requireNonNull(event), Objects.requireNonNull(user), Objects.requireNonNull(category), role);
        return toDto(eventParticipationRepository.save(Objects.requireNonNull(participation)));
    }

    public EventParticipationDto registerCompetitor(Long eventId, Long userId, Long categoryId) {
        return registerParticipation(eventId, userId, categoryId, ParticipationRole.COMPETITOR);
    }

    public EventParticipationDto registerVoter(Long eventId, Long userId, Long categoryId) {
        return registerParticipation(eventId, userId, categoryId, ParticipationRole.VOTER);
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

    public List<EventParticipationDto> getVotersByEventAndCategory(Long eventId, Long categoryId) {
        List<EventParticipation> participations = eventParticipationRepository
                .findByEventIdAndCategoryIdAndRole(eventId, categoryId, ParticipationRole.VOTER);
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

    public void removeParticipation(Long eventId, Long userId, Long categoryId) {
        if (eventId == null || userId == null || categoryId == null) {
            throw new RuntimeException("IDs are required for removal");
        }
        EventParticipation participation = eventParticipationRepository
                .findByEventIdAndUserIdAndCategoryId(
                        Objects.requireNonNull(eventId), Objects.requireNonNull(userId), Objects.requireNonNull(categoryId))
                .orElseThrow(() -> new RuntimeException("Participation not found"));
        eventParticipationRepository.delete(Objects.requireNonNull(participation));
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
