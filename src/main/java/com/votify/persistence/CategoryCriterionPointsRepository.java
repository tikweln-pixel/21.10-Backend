package com.votify.persistence;

import com.votify.entity.CategoryCriterionPoints;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryCriterionPointsRepository extends JpaRepository<CategoryCriterionPoints, Long> {

    List<CategoryCriterionPoints> findByCategoryId(Long categoryId);

    Optional<CategoryCriterionPoints> findByCategoryIdAndCriterionId(Long categoryId, Long criterionId);

    void deleteByCategoryId(Long categoryId);
}
