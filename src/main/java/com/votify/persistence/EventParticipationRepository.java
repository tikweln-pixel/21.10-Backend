package com.votify.persistence;

import com.votify.entity.EventParticipation;
import com.votify.entity.ParticipationRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventParticipationRepository extends JpaRepository<EventParticipation, Long> {

    List<EventParticipation> findByEventId(Long eventId);

    List<EventParticipation> findByEventIdAndCategoryId(Long eventId, Long categoryId);

    List<EventParticipation> findByEventIdAndCategoryIdAndRole(Long eventId, Long categoryId, ParticipationRole role);

    List<EventParticipation> findByUserId(Long userId);

    List<EventParticipation> findByEventIdAndUserId(Long eventId, Long userId);

    Optional<EventParticipation> findByEventIdAndUserIdAndCategoryId(Long eventId, Long userId, Long categoryId);

    boolean existsByEventIdAndUserId(Long eventId, Long userId);

    boolean existsByEventIdAndUserIdAndCategoryId(Long eventId, Long userId, Long categoryId);

    void deleteByEventId(Long eventId);

    void deleteByCategoryId(Long categoryId);
}
