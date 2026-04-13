package com.votify.persistence;

import com.votify.entity.CategoryCriterionPoints;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CategoryCriterionPointsRepository extends JpaRepository<CategoryCriterionPoints, Long> {

    List<CategoryCriterionPoints> findByCategoryId(Long categoryId);

    Optional<CategoryCriterionPoints> findByCategoryIdAndCriterionId(Long categoryId, Long criterionId);

    @Modifying
    @Query("DELETE FROM CategoryCriterionPoints c WHERE c.category.id = :categoryId")
    void deleteByCategoryId(Long categoryId);
}
