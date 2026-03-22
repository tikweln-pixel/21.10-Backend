package com.votify.service;

import com.votify.dto.CreateEventRequest;
import com.votify.dto.EventDto;
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

    @Transactional
    public EventDto create(CreateEventRequest request) {
        if (request.getCreatorUserId() == null) {
            throw new RuntimeException("creatorUserId is required");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new RuntimeException("Event name is required");
        }
        if (request.getCategoryNames() == null || request.getCategoryNames().isEmpty()) {
            throw new RuntimeException("At least one category is required");
        }
        String creatorCat = request.getCreatorCategoryName() == null ? "" : request.getCreatorCategoryName().trim();
        if (creatorCat.isEmpty()) {
            throw new RuntimeException("Creator category is required");
        }
        boolean creatorMatches = request.getCategoryNames().stream()
                .filter(n -> n != null && !n.isBlank())
                .anyMatch(n -> n.trim().equalsIgnoreCase(creatorCat));
        if (!creatorMatches) {
            throw new RuntimeException("Creator category must be one of the event categories");
        }

        Event event = new Event(request.getName().trim());
        event.setTimeInitial(request.getTimeInitial());
        event.setTimeFinal(request.getTimeFinal());
        event = eventRepository.save(event);

        for (String categoryName : request.getCategoryNames()) {
            if (categoryName == null || categoryName.isBlank()) {
                continue;
            }
            Category category = new Category(categoryName.trim(), event);
            category.setTimeInitial(request.getTimeInitial());
            category.setTimeFinal(request.getTimeFinal());
            Integer reminderMinutes = resolveReminderMinutes(request);
            if (reminderMinutes != null) {
                category.setReminderMinutes(reminderMinutes);
            }
            event.getCategories().add(category);
        }
        if (event.getCategories().isEmpty()) {
            throw new RuntimeException("At least one non-empty category name is required");
        }
        eventRepository.save(event);

        Category creatorCategory = event.getCategories().stream()
                .filter(c -> c.getName().equalsIgnoreCase(creatorCat))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Creator category not found"));
        eventParticipationService.registerCompetitor(event.getId(), request.getCreatorUserId(), creatorCategory.getId());

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
        Long organizerId = event.getOrganizer() != null ? event.getOrganizer().getId() : null;
        return new EventDto(event.getId(), event.getName(), event.getTimeInitial(), event.getTimeFinal(), organizerId);
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

    private static Integer resolveReminderMinutes(CreateEventRequest request) {
        if (request.getReminderMinutes() != null) {
            return request.getReminderMinutes();
        }
        if (request.getReminderHours() != null) {
            return request.getReminderHours() * 60;
        }
        return null;
    }
}
