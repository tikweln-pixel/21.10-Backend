package com.votify.persistence;

import com.votify.entity.Voting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VotingRepository extends JpaRepository<Voting, Long> {

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

    @Query("SELECT v FROM Voting v WHERE v.voter.id = :voterId AND v.project.id = :projectId "
           + "AND v.criterion.id = :criterionId AND ((:categoryId IS NULL AND v.category IS NULL) OR v.category.id = :categoryId)")
    Optional<Voting> findExistingVote(@Param("voterId") Long voterId,
                                      @Param("projectId") Long projectId,
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

    List<Voting> findByProjectIdIn(List<Long> projectIds);

    List<Voting> findByVoterIdAndProjectId(Long voterId, Long projectId);

    @Query("SELECT DISTINCT v.voter.id FROM Voting v WHERE v.category.id = :categoryId")
    List<Long> findDistinctVoterIdsByCategoryId(@Param("categoryId") Long categoryId);

    List<Voting> findByVoterIdAndProjectIdAndCategoryId(Long voterId, Long projectId, Long categoryId);

    List<Voting> findByProjectIdInAndCategoryId(List<Long> projectIds, Long categoryId);

    /* ── Ranking de proyectos por categoría ── */

    @Query("SELECT v.project.id, SUM(v.score) FROM Voting v " +
           "WHERE v.category.id = :categoryId " +
           "GROUP BY v.project.id " +
           "ORDER BY SUM(v.score) DESC")
    List<Object[]> findProjectScoresByCategoryId(@Param("categoryId") Long categoryId);
}
