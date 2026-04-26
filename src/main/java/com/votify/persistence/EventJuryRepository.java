package com.votify.persistence;

import com.votify.entity.EventJury;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventJuryRepository extends JpaRepository<EventJury, Long> {

    List<EventJury> findByEventId(Long eventId);

    Optional<EventJury> findByEventIdAndUserId(Long eventId, Long userId);

    boolean existsByEventIdAndUserId(Long eventId, Long userId);

    void deleteByEventId(Long eventId);
}
