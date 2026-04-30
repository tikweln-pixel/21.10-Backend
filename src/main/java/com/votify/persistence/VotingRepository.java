package com.votify.persistence;

import com.votify.entity.Voting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VotingRepository extends JpaRepository<Voting, Long> {

    @Query("SELECT COUNT(DISTINCT v.competitor.id) FROM Voting v " +
           "WHERE v.voter.id = :voterId AND v.category.id = :categoryId")
    long countDistinctCompetitorsByVoterIdAndCategoryId(
            @Param("voterId") Long voterId,
            @Param("categoryId") Long categoryId);

    @Query("SELECT COALESCE(SUM(v.score), 0) FROM Voting v " +
           "WHERE v.voter.id = :voterId AND v.category.id = :categoryId")
    int sumScoreByVoterIdAndCategoryId(
            @Param("voterId") Long voterId,
            @Param("categoryId") Long categoryId);

    List<Voting> findByVoterIdAndCategoryId(Long voterId, Long categoryId);

    @Query("SELECT v FROM Voting v WHERE v.voter.id = :voterId AND v.competitor.id = :competitorId "
           + "AND v.criterion.id = :criterionId AND ((:categoryId IS NULL AND v.category IS NULL) OR v.category.id = :categoryId)")
    Optional<Voting> findExistingVote(@Param("voterId") Long voterId,
                                      @Param("competitorId") Long competitorId,
                                      @Param("criterionId") Long criterionId,
                                      @Param("categoryId") Long categoryId);

    /* ── Soporte para eliminación en cascada ── */

    @Modifying
    @Query("DELETE FROM Voting v WHERE v.category.id IN :categoryIds")
    void deleteByCategoryIdIn(@Param("categoryIds") List<Long> categoryIds);

    @Modifying
    @Query("DELETE FROM Voting v WHERE v.category.id = :categoryId")
    void deleteByCategoryId(@Param("categoryId") Long categoryId);

    @Modifying
    @Query("DELETE FROM Voting v WHERE v.criterion.id = :criterionId")
    void deleteByCriterionId(@Param("criterionId") Long criterionId);

    /* ── Consultas para el frontend ── */

    List<Voting> findByCompetitorIdIn(List<Long> competitorIds);

    List<Voting> findByVoterIdAndCompetitorId(Long voterId, Long competitorId);

    @Query("SELECT DISTINCT v.voter.id FROM Voting v WHERE v.category.id = :categoryId")
    List<Long> findDistinctVoterIdsByCategoryId(@Param("categoryId") Long categoryId);

    List<Voting> findByVoterIdAndCompetitorIdAndCategoryId(Long voterId, Long competitorId, Long categoryId);

    List<Voting> findByCompetitorIdInAndCategoryId(List<Long> competitorIds, Long categoryId);

    /* ── Ranking de competidores por categoría ── */

    @Query("SELECT v.competitor.id, COALESCE(SUM(CASE WHEN v.weightedScore IS NOT NULL THEN v.weightedScore ELSE CAST(v.score AS DOUBLE) END), 0) " +
           "FROM Voting v " +
           "WHERE v.category.id = :categoryId " +
           "GROUP BY v.competitor.id " +
           "ORDER BY SUM(CASE WHEN v.weightedScore IS NOT NULL THEN v.weightedScore ELSE CAST(v.score AS DOUBLE) END) DESC")
    List<Object[]> findCompetitorScoresByCategoryId(@Param("categoryId") Long categoryId);
}
