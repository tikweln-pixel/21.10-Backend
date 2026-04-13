package com.votify.persistence;

import com.votify.entity.Competitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CompetitorRepository extends JpaRepository<Competitor, Long> {
    Optional<Competitor> findByEmail(String email);

    /** Promueve un User existente a Competitor insertando solo en la tabla competitors. */
    @Modifying
    @Query(value = "INSERT INTO competitors (user_id) VALUES (:userId) ON CONFLICT DO NOTHING", nativeQuery = true)
    void insertCompetitorRow(@Param("userId") Long userId);
}
