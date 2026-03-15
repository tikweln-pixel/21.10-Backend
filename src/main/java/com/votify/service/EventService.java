package com.votify.service;

import com.votify.dto.CreateEventRequest;
import com.votify.dto.EventDto;
import com.votify.entity.Category;
import com.votify.entity.Event;
import com.votify.persistence.EventRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final EventParticipationService eventParticipationService;

    public EventService(EventRepository eventRepository, EventParticipationService eventParticipationService) {
        this.eventRepository = eventRepository;
        this.eventParticipationService = eventParticipationService;
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

        Event event = new Event(request.getName());
        event = eventRepository.save(event);

        for (String categoryName : request.getCategoryNames()) {
            Category category = new Category(categoryName, event);
            event.getCategories().add(category);
        }
        eventRepository.save(event);

        Category creatorCategory = event.getCategories().stream()
                .filter(c -> c.getName().equals(request.getCreatorCategoryName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Creator category not found"));
        eventParticipationService.registerCompetitor(event.getId(), request.getCreatorUserId(), creatorCategory.getId());

        return toDto(event);
    }

    public EventDto update(Long id, EventDto dto) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
        event.setName(dto.getName());
        return toDto(eventRepository.save(event));
    }

    public void delete(Long id) {
        eventRepository.deleteById(id);
    }

    private EventDto toDto(Event event) {
        return new EventDto(event.getId(), event.getName());
    }
}
