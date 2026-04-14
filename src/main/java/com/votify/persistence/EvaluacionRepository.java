package com.votify.persistence;

import com.votify.entity.Evaluacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EvaluacionRepository extends JpaRepository<Evaluacion, Long> {

    List<Evaluacion> findByCategoryId(Long categoryId);

    List<Evaluacion> findByCompetitorId(Long competitorId);

    List<Evaluacion> findByEvaluadorId(Long evaluadorId);

    List<Evaluacion> findByCategoryIdAndCompetitorId(Long categoryId, Long competitorId);

    @Modifying
    @Query("DELETE FROM Evaluacion e WHERE e.category.id = :categoryId")
    void deleteByCategoryId(@Param("categoryId") Long categoryId);

    @Modifying
    @Query("DELETE FROM Evaluacion e WHERE e.criterion.id = :criterionId")
    void deleteByCriterionId(@Param("criterionId") Long criterionId);
}
