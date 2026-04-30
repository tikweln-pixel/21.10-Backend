package com.votify.persistence;

import com.votify.entity.Voting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VotingRepository extends JpaRepository<Voting, Long> {

    // ── Validaciones de restricciones de voto popular ──────────────────────

    @Query("SELECT COUNT(DISTINCT v.project.id) FROM Voting v " +
           "WHERE v.voter.id = :voterId AND v.category.id = :categoryId")
    long countDistinctProjectsByVoterIdAndCategoryId(
            @Param("voterId") Long voterId,
            @Param("categoryId") Long categoryId);

    @Query("SELECT COALESCE(SUM(v.score), 0) FROM Voting v " +
           "WHERE v.voter.id = :voterId AND v.category.id = :categoryId")
    int sumScoreByVoterIdAndCategoryId(
            @Param("voterId") Long voterId,
            @Param("categoryId") Long categoryId);

    List<Voting> findByVoterIdAndCategoryId(Long voterId, Long categoryId);

    // ── Deduplicación de votos ─────────────────────────────────────────────

    @Query("SELECT v FROM Voting v WHERE v.voter.id = :voterId AND v.project.id = :projectId "
           + "AND v.criterion.id = :criterionId AND ((:categoryId IS NULL AND v.category IS NULL) OR v.category.id = :categoryId)")
    Optional<Voting> findExistingVote(@Param("voterId") Long voterId,
                                      @Param("projectId") Long projectId,
                                      @Param("criterionId") Long criterionId,
                                      @Param("categoryId") Long categoryId);

    // ── Eliminación en cascada ─────────────────────────────────────────────

    @Modifying
    @Query("DELETE FROM Voting v WHERE v.category.id IN :categoryIds")
    void deleteByCategoryIdIn(@Param("categoryIds") List<Long> categoryIds);

    @Modifying
    @Query("DELETE FROM Voting v WHERE v.category.id = :categoryId")
    void deleteByCategoryId(@Param("categoryId") Long categoryId);

    @Modifying
    @Query("DELETE FROM Voting v WHERE v.criterion.id = :criterionId")
    void deleteByCriterionId(@Param("criterionId") Long criterionId);

    // ── Consultas para el frontend ─────────────────────────────────────────

    List<Voting> findByProjectIdAndComentarioIsNotNull(Long projectId);

    List<Voting> findByProjectIdIn(List<Long> projectIds);

    List<Voting> findByProjectIdInAndCategoryId(List<Long> projectIds, Long categoryId);

    List<Voting> findByVoterIdAndProjectId(Long voterId, Long projectId);

    List<Voting> findByVoterIdAndProjectIdAndCategoryId(Long voterId, Long projectId, Long categoryId);

    @Query("SELECT DISTINCT v.voter.id FROM Voting v WHERE v.category.id = :categoryId")
    List<Long> findDistinctVoterIdsByCategoryId(@Param("categoryId") Long categoryId);

    // ── Ranking de proyectos por categoría ────────────────────────────────

    @Query("SELECT v.project.id, COALESCE(SUM(CASE WHEN v.weightedScore IS NOT NULL THEN v.weightedScore ELSE CAST(v.score AS DOUBLE) END), 0) " +
           "FROM Voting v " +
           "WHERE v.category.id = :categoryId " +
           "GROUP BY v.project.id " +
           "ORDER BY SUM(CASE WHEN v.weightedScore IS NOT NULL THEN v.weightedScore ELSE CAST(v.score AS DOUBLE) END) DESC")
    List<Object[]> findProjectScoresByCategoryId(@Param("categoryId") Long categoryId);
}
