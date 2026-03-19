package com.votify.service;

import com.votify.dto.EventParticipationDto;
import com.votify.entity.*;
import com.votify.persistence.CategoryRepository;
import com.votify.persistence.EventParticipationRepository;
import com.votify.persistence.EventRepository;
import com.votify.persistence.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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

    /**
     * Registers a user as competitor or voter in a specific category of an event.
     * A user can have different roles in different categories of the same event.
     */
    public EventParticipationDto registerParticipation(Long eventId, Long userId, Long categoryId, ParticipationRole role) {
        if (categoryId == null) {
            throw new RuntimeException("Category is required for participation");
        }
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));

        if (!category.getEvent().getId().equals(eventId)) {
            throw new RuntimeException("Category does not belong to this event");
        }

        if (eventParticipationRepository.existsByEventIdAndUserIdAndCategoryId(eventId, userId, categoryId)) {
            throw new RuntimeException("User " + userId + " is already registered in category " + categoryId + " of event " + eventId);
        }

        EventParticipation participation = new EventParticipation(event, user, category, role);
        return toDto(eventParticipationRepository.save(participation));
    }

    public EventParticipationDto registerCompetitor(Long eventId, Long userId, Long categoryId) {
        return registerParticipation(eventId, userId, categoryId, ParticipationRole.COMPETITOR);
    }

    public EventParticipationDto registerVoter(Long eventId, Long userId, Long categoryId) {
        return registerParticipation(eventId, userId, categoryId, ParticipationRole.VOTER);
    }

    public List<EventParticipationDto> getParticipationsByEvent(Long eventId) {
        return eventParticipationRepository.findByEventId(eventId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<EventParticipationDto> getParticipationsByEventAndCategory(Long eventId, Long categoryId) {
        return eventParticipationRepository.findByEventIdAndCategoryId(eventId, categoryId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<EventParticipationDto> getCompetitorsByEventAndCategory(Long eventId, Long categoryId) {
        return eventParticipationRepository.findByEventIdAndCategoryIdAndRole(eventId, categoryId, ParticipationRole.COMPETITOR)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<EventParticipationDto> getVotersByEventAndCategory(Long eventId, Long categoryId) {
        return eventParticipationRepository.findByEventIdAndCategoryIdAndRole(eventId, categoryId, ParticipationRole.VOTER)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<EventParticipationDto> getParticipationsByUser(Long userId) {
        return eventParticipationRepository.findByUserId(userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public void removeParticipation(Long eventId, Long userId, Long categoryId) {
        EventParticipation participation = eventParticipationRepository
                .findByEventIdAndUserIdAndCategoryId(eventId, userId, categoryId)
                .orElseThrow(() -> new RuntimeException("Participation not found"));
        eventParticipationRepository.delete(participation);
    }

    private EventParticipationDto toDto(EventParticipation participation) {
        return new EventParticipationDto(
                participation.getId(),
                participation.getEvent().getId(),
                participation.getUser().getId(),
                participation.getCategory().getId(),
                participation.getRole()
        );
    }
}
