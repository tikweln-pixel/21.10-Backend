package com.votify.persistence;

import com.votify.entity.Voting;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
