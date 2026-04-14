package com.votify.persistence;

import com.votify.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByEventId(Long eventId);

    List<Project> findByCategoryId(Long categoryId);
}

