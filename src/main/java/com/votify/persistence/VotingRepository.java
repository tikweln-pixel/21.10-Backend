package com.votify.persistence;

import com.votify.entity.Voting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VotingRepository extends JpaRepository<Voting, Long> {

    /**
     * Req. 19 – Restricción POPULAR_VOTE:
     * Cuenta cuántos competidores distintos ha votado un votante en una categoría concreta.
     * Se usa para validar que no supera el límite maxVotesPerVoter de la categoría.
     */
    @Query("SELECT COUNT(DISTINCT v.competitor.id) FROM Voting v " +
           "WHERE v.voter.id = :voterId AND v.category.id = :categoryId")
    long countDistinctCompetitorsByVoterIdAndCategoryId(
            @Param("voterId") Long voterId,
            @Param("categoryId") Long categoryId);

    /**
     * Req. 23 – Validación puntos POPULAR_VOTE:
     * Suma total de puntos ya asignados por un votante en una categoría.
     * Se usa para verificar que no supera el totalPoints configurado.
     */
    @Query("SELECT COALESCE(SUM(v.score), 0) FROM Voting v " +
           "WHERE v.voter.id = :voterId AND v.category.id = :categoryId")
    int sumScoreByVoterIdAndCategoryId(
            @Param("voterId") Long voterId,
            @Param("categoryId") Long categoryId);

    /**
     * Devuelve todos los votos de un votante en una categoría (para auditoría y consulta).
     */
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

    /** Votos de un jurado (voterId) sobre un competidor en una categoría concreta. */
    List<Voting> findByVoterIdAndCompetitorIdAndCategoryId(Long voterId, Long competitorId, Long categoryId);

    /** Todos los votos de una lista de competidores en una categoría (para calcular nota final). */
    List<Voting> findByCompetitorIdInAndCategoryId(List<Long> competitorIds, Long categoryId);
}
