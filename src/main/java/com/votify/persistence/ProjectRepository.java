package com.votify.persistence;

import com.votify.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByEventId(Long eventId);

    List<Project> findByCategoryId(Long categoryId);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
           "FROM Project p JOIN p.competitors voterComp JOIN p.competitors targetComp " +
           "WHERE p.event.id = :eventId AND voterComp.id = :voterId AND targetComp.id = :targetCompetitorId")
    boolean existsSharedProjectInEvent(@Param("eventId") Long eventId,
                                       @Param("voterId") Long voterId,
                                       @Param("targetCompetitorId") Long targetCompetitorId);
}

