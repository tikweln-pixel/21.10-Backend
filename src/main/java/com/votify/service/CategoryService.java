package com.votify.service;

import com.votify.dto.CategoryDto;
import com.votify.entity.Category;
import com.votify.entity.Event;
import com.votify.persistence.CategoryRepository;
import com.votify.persistence.EventRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;

    public CategoryService(CategoryRepository categoryRepository, EventRepository eventRepository) {
        this.categoryRepository = categoryRepository;
        this.eventRepository = eventRepository;
    }

    public List<CategoryDto> findAll() {
        return categoryRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<CategoryDto> findByEventId(Long eventId) {
        return categoryRepository.findByEventId(eventId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public CategoryDto findById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
        return toDto(category);
    }

    public CategoryDto createForEvent(Long eventId, String name) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));
        Category category = new Category(name, event);
        return toDto(categoryRepository.save(category));
    }

    public CategoryDto create(CategoryDto dto) {
        if (dto.getEventId() == null) {
            throw new RuntimeException("Event is required for category creation");
        }
        Event event = eventRepository.findById(dto.getEventId())
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + dto.getEventId()));

        validateCategoryTimesWithinEvent(event, dto.getTimeInitial(), dto.getTimeFinal());

        Category category = new Category(dto.getName(), event);
        category.setTimeInitial(dto.getTimeInitial());
        category.setTimeFinal(dto.getTimeFinal());
        category.setReminderMinutes(dto.getReminderMinutes());
        return toDto(categoryRepository.save(category));
    }

    public CategoryDto update(Long id, CategoryDto dto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));

        if (dto.getEventId() != null && (category.getEvent() == null || !dto.getEventId().equals(category.getEvent().getId()))) {
            Event event = eventRepository.findById(dto.getEventId())
                    .orElseThrow(() -> new RuntimeException("Event not found with id: " + dto.getEventId()));
            category.setEvent(event);
        }

        Event event = category.getEvent();
        validateCategoryTimesWithinEvent(event, dto.getTimeInitial(), dto.getTimeFinal());

        category.setName(dto.getName());
        category.setTimeInitial(dto.getTimeInitial());
        category.setTimeFinal(dto.getTimeFinal());
        category.setReminderMinutes(dto.getReminderMinutes());
        return toDto(categoryRepository.save(category));
    }

    public void delete(Long id) {
        categoryRepository.deleteById(id);
    }

    private CategoryDto toDto(Category category) {
        Long eventId = category.getEvent() != null ? category.getEvent().getId() : null;
        return new CategoryDto(category.getId(), category.getName(), category.getTimeInitial(), category.getTimeFinal(), eventId, category.getReminderMinutes());
    }

    private void validateCategoryTimesWithinEvent(Event event, Date start, Date end) {
        if (event == null) return;

        Date evStart = event.getTimeInitial();
        Date evEnd = event.getTimeFinal();

        if (start != null && evStart != null && start.before(evStart)) {
            throw new RuntimeException("Category start time cannot be before event start time");
        }
        if (end != null && evEnd != null && end.after(evEnd)) {
            throw new RuntimeException("Category end time cannot be after event end time");
        }
        if (start != null && end != null && end.before(start)) {
            throw new RuntimeException("Category end time cannot be before its start time");
        }
    }

    public CategoryDto setTimeInitial(Long id, Date timeInitial) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
        validateCategoryTimesWithinEvent(category.getEvent(), timeInitial, category.getTimeFinal());
        category.setTimeInitial(timeInitial);
        return toDto(categoryRepository.save(category));
    }

    public CategoryDto setTimeFinal(Long id, Date timeFinal) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
        validateCategoryTimesWithinEvent(category.getEvent(), category.getTimeInitial(), timeFinal);
        category.setTimeFinal(timeFinal);
        return toDto(categoryRepository.save(category));
    }
}
