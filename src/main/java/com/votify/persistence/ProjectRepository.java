package com.votify.persistence;

import com.votify.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByEventId(Long eventId);

    List<Project> findByCategoryId(Long categoryId);

    @Query("SELECT p FROM Project p JOIN p.competitors c WHERE c.id = :competitorId")
    List<Project> findByCompetitorId(@Param("competitorId") Long competitorId);
}

