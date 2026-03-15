package com.votify.service;

import com.votify.dto.EventDto;
import com.votify.entity.Event;
import com.votify.entity.User;
import com.votify.persistence.EventRepository;
import com.votify.persistence.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public EventService(EventRepository eventRepository, UserRepository userRepository) {
        this.eventRepository = eventRepository;
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
}
