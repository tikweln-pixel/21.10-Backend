package com.votify.service;

import com.votify.dto.TimeWindowDto;
import com.votify.entity.TimeWindow;
import com.votify.persistence.TimeWindowRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class TimeWindowService {

    private final TimeWindowRepository timeWindowRepository;

    public TimeWindowService(TimeWindowRepository timeWindowRepository) {
        this.timeWindowRepository = timeWindowRepository;
    }

    public List<TimeWindowDto> findAll() {
        List<TimeWindowDto> result = new ArrayList<>();
        for (TimeWindow tw : timeWindowRepository.findAll()) {
            result.add(toDto(tw));
        }
        return result;
    }

    public TimeWindowDto findById(Long id) {
        if (id == null) throw new RuntimeException("El ID de la ventana de tiempo no puede ser nulo");
        TimeWindow timeWindow = timeWindowRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("TimeWindow not found with id: " + id));
        return toDto(timeWindow);
    }

    public TimeWindowDto create(TimeWindowDto dto) {
        TimeWindow timeWindow = new TimeWindow(dto.getStartTime(), dto.getEndTime());
        return toDto(timeWindowRepository.save(Objects.requireNonNull(timeWindow)));
    }

    public TimeWindowDto update(Long id, TimeWindowDto dto) {
        if (id == null) throw new RuntimeException("El ID de la ventana de tiempo no puede ser nulo");
        TimeWindow timeWindow = timeWindowRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("TimeWindow not found with id: " + id));
        timeWindow.setStartTime(dto.getStartTime());
        timeWindow.setEndTime(dto.getEndTime());
        return toDto(timeWindowRepository.save(Objects.requireNonNull(timeWindow)));
    }

    public void delete(Long id) {
        if (id == null) throw new RuntimeException("El ID de la ventana de tiempo no puede ser nulo");
        timeWindowRepository.deleteById(Objects.requireNonNull(id));
    }

    private TimeWindowDto toDto(TimeWindow timeWindow) {
        return new TimeWindowDto(timeWindow.getId(), timeWindow.getStartTime(), timeWindow.getEndTime());
    }
}
