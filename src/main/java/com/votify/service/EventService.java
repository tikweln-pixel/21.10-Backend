package com.votify.service;


import com.votify.dto.CategoryDto;
import com.votify.dto.EventDto;
import com.votify.dto.UserDto;
import com.votify.entity.Category;
import com.votify.entity.Event;
import com.votify.entity.User;
import com.votify.persistence.EventRepository;
import com.votify.persistence.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final EventParticipationService eventParticipationService;
    private final UserRepository userRepository;

    public EventService(EventRepository eventRepository, EventParticipationService eventParticipationService, UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.eventParticipationService = eventParticipationService;
        this.userRepository = userRepository;
    }

    public List<EventDto> findAll() {
        return eventRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public EventDto findById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
        return toDto(event);
    }

    public EventDto create(CreateEventRequest request) {
        if (request.getCategoryNames() == null || request.getCategoryNames().isEmpty()) {
            throw new RuntimeException("At least one category is required");
        }
        if (request.getCreatorCategoryName() == null || !request.getCategoryNames().contains(request.getCreatorCategoryName())) {
            throw new RuntimeException("Creator category must be one of the event categories");
        }

        Event event = new Event(dto.getName().trim());
        event.setTimeInitial(dto.getTimeInitial());
        event.setTimeFinal(dto.getTimeFinal());
        event = eventRepository.save(event);

        Category firstCategory = null;
        for (CategoryDto cd : incoming) {
            if (cd == null || cd.getName() == null || cd.getName().isBlank()) {
                continue;
            }
            Category category = new Category(cd.getName().trim(), event);
            category.setTimeInitial(dto.getTimeInitial());
            category.setTimeFinal(dto.getTimeFinal());
            if (cd.getVotingType() != null) {
                category.setVotingType(cd.getVotingType());
            }
            Integer reminderMinutes = resolveReminderMinutes(dto);
            if (reminderMinutes != null) {
                category.setReminderMinutes(reminderMinutes);
            }
            if (cd.getReminderMinutes() != null) {
                category.setReminderMinutes(cd.getReminderMinutes());
            }
            if (cd.getTotalPoints() != null) {
                category.setTotalPoints(cd.getTotalPoints());
            }
            if (cd.getMaxVotesPerVoter() != null) {
                category.setMaxVotesPerVoter(cd.getMaxVotesPerVoter());
            }
            event.getCategories().add(category);
            if (firstCategory == null) {
                firstCategory = category;
            }
        }
        if (event.getCategories().isEmpty()) {
            throw new RuntimeException("At least one non-empty category name is required");
        }
        eventRepository.save(event);

        eventParticipationService.registerCompetitor(event.getId(), creatorId, firstCategory.getId());

        return toDto(event);
    }

    public EventDto createForOrganizer(Long organizerId, EventDto dto) {
        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new RuntimeException("User (organizer) not found with id: " + organizerId));

        Event event = organizer.createEvent(dto.getName(), dto.getTimeInitial(), dto.getTimeFinal());
        return toDto(eventRepository.save(event));
    }

    public EventDto update(Long id, EventDto dto) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
        event.setName(dto.getName());
        event.setTimeInitial(dto.getTimeInitial());
        event.setTimeFinal(dto.getTimeFinal());
        if (dto.getOrganizerId() != null) {
            User organizer = userRepository.findById(dto.getOrganizerId())
                    .orElseThrow(() -> new RuntimeException("User (organizer) not found with id: " + dto.getOrganizerId()));
            event.setOrganizer(organizer);
        }
        return toDto(eventRepository.save(event));
    }

    public void delete(Long id) {
        eventRepository.deleteById(id);
    }

    private EventDto toDto(Event event) {
        UserDto creatorDto = null;
        Long organizerId = null;
        if (event.getOrganizer() != null) {
            User org = event.getOrganizer();
            organizerId = org.getId();
            creatorDto = new UserDto(org.getId(), org.getName(), org.getEmail());
        }
        List<CategoryDto> categoryDtos = event.getCategories().stream()
                .map(this::categoryToDto)
                .collect(Collectors.toList());
        EventDto dto = new EventDto(
                event.getId(),
                event.getName(),
                event.getTimeInitial(),
                event.getTimeFinal(),
                creatorDto,
                categoryDtos
        );
        dto.setOrganizerId(organizerId);
        return dto;
    }

    private CategoryDto categoryToDto(Category c) {
        Long eventId = c.getEvent() != null ? c.getEvent().getId() : null;
        return new CategoryDto(
                c.getId(),
                c.getName(),
                c.getVotingType(),
                c.getTimeInitial(),
                c.getTimeFinal(),
                eventId,
                c.getReminderMinutes(),
                c.getTotalPoints(),
                c.getMaxVotesPerVoter()
        );
    }

    public EventDto setTimeInitial(Long id, Date timeInitial) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
        event.setTimeInitial(timeInitial);
        return toDto(eventRepository.save(event));
    }

    public EventDto setTimeFinal(Long id, Date timeFinal) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
        event.setTimeFinal(timeFinal);
        return toDto(eventRepository.save(event));
    }

    private static Integer resolveReminderMinutes(EventDto dto) {
        if (dto.getReminderMinutes() != null) {
            return dto.getReminderMinutes();
        }
        if (dto.getReminderHours() != null) {
            return dto.getReminderHours() * 60;
        }
        return null;
    }
}
