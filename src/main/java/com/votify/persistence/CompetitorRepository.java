package com.votify.persistence;

import com.votify.entity.Competitor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompetitorRepository extends JpaRepository<Competitor, Long> {
}
