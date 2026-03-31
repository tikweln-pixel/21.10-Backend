package com.votify.service;

import com.votify.dto.EventParticipationDto;
import com.votify.entity.*;
import com.votify.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class EventParticipationService {

    private static final Pattern EMAIL_REGEX = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final EventParticipationRepository eventParticipationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final CompetitorRepository competitorRepository;
    private final VoterRepository voterRepository;

    public EventParticipationService(EventParticipationRepository eventParticipationRepository,
                                     EventRepository eventRepository,
                                     UserRepository userRepository,
                                     CategoryRepository categoryRepository,
                                     CompetitorRepository competitorRepository,
                                     VoterRepository voterRepository) {
        this.eventParticipationRepository = eventParticipationRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.competitorRepository = competitorRepository;
        this.voterRepository = voterRepository;
    }

    /**
     * Registra a un usuario como competidor o votante en una categoría concreta de un evento.
     * Un mismo usuario puede tener distintos roles en distintas categorías del mismo evento.
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

    @Transactional
    public EventParticipationDto registerNewCompetitor(Long eventId, String name, String email, Long categoryId) {
        validateNewParticipant(name, email);
        Competitor competitor = competitorRepository.save(new Competitor(name.trim(), email.trim()));
        return registerParticipation(eventId, competitor.getId(), categoryId, ParticipationRole.COMPETITOR);
    }

    @Transactional
    public EventParticipationDto registerNewVoter(Long eventId, String name, String email, Long categoryId) {
        validateNewParticipant(name, email);
        Voter voter = voterRepository.save(new Voter(name.trim(), email.trim()));
        return registerParticipation(eventId, voter.getId(), categoryId, ParticipationRole.VOTER);
    }

    private void validateNewParticipant(String name, String email) {
        if (name == null || name.isBlank()) {
            throw new RuntimeException("Name is required");
        }
        if (email == null || !EMAIL_REGEX.matcher(email).matches()) {
            throw new RuntimeException("Valid email is required");
        }
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
