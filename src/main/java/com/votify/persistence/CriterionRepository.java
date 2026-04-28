package com.votify.persistence;

import com.votify.entity.Criterion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CriterionRepository extends JpaRepository<Criterion, Long> {

    List<Criterion> findByCategoryId(Long categoryId);
}
