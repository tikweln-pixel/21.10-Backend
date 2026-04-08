package com.votify.persistence;

import com.votify.entity.Competitor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompetitorRepository extends JpaRepository<Competitor, Long> {
    Optional<Competitor> findByEmail(String email);
}
