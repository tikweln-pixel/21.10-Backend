package com.votify.service;

import com.votify.dto.TimeWindowDto;
import com.votify.entity.TimeWindow;
import com.votify.persistence.TimeWindowRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TimeWindowService {

    private final TimeWindowRepository timeWindowRepository;

    public TimeWindowService(TimeWindowRepository timeWindowRepository) {
        this.timeWindowRepository = timeWindowRepository;
    }

    public List<TimeWindowDto> findAll() {
        return timeWindowRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public TimeWindowDto findById(Long id) {
        TimeWindow timeWindow = timeWindowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("TimeWindow not found with id: " + id));
        return toDto(timeWindow);
    }

    public TimeWindowDto create(TimeWindowDto dto) {
        TimeWindow timeWindow = new TimeWindow(dto.getStartTime(), dto.getEndTime());
        return toDto(timeWindowRepository.save(timeWindow));
    }

    public TimeWindowDto update(Long id, TimeWindowDto dto) {
        TimeWindow timeWindow = timeWindowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("TimeWindow not found with id: " + id));
        timeWindow.setStartTime(dto.getStartTime());
        timeWindow.setEndTime(dto.getEndTime());
        return toDto(timeWindowRepository.save(timeWindow));
    }

    public void delete(Long id) {
        timeWindowRepository.deleteById(id);
    }

    private TimeWindowDto toDto(TimeWindow timeWindow) {
        return new TimeWindowDto(timeWindow.getId(), timeWindow.getStartTime(), timeWindow.getEndTime());
    }
}
