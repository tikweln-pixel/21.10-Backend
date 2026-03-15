package com.votify.service;

import com.votify.dto.CategoryDto;
import com.votify.entity.Category;
import com.votify.entity.Event;
import com.votify.persistence.CategoryRepository;
import com.votify.persistence.EventRepository;
import org.springframework.stereotype.Service;

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
        return createForEvent(dto.getEventId(), dto.getName());
    }

    public CategoryDto update(Long id, CategoryDto dto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
        category.setName(dto.getName());
        return toDto(categoryRepository.save(category));
    }

    public void delete(Long id) {
        categoryRepository.deleteById(id);
    }

    private CategoryDto toDto(Category category) {
        Long eventId = category.getEvent() != null ? category.getEvent().getId() : null;
        return new CategoryDto(category.getId(), category.getName(), eventId);
    }
}
