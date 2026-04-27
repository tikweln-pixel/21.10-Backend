package com.votify.persistence;

import com.votify.entity.TimeWindow;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimeWindowRepository extends JpaRepository<TimeWindow, Long> {
}
