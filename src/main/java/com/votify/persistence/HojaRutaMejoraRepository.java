package com.votify.persistence;

import com.votify.entity.HojaRutaMejora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HojaRutaMejoraRepository extends JpaRepository<HojaRutaMejora, Long> {

    List<HojaRutaMejora> findByCompetitorId(Long competitorId);

    Optional<HojaRutaMejora> findByCompetitorIdAndCategoryId(Long competitorId, Long categoryId);

    Optional<HojaRutaMejora> findByCompetitorIdAndCategoryIsNull(Long competitorId);

    /**
     * DELETE directo via JPQL con @Modifying para evitar el problema de orden de flush
     * de Hibernate (los derived deletes se encolan como DELETE pero Hibernate ejecuta
     * primero los INSERT pendientes, violando el unique constraint uk_hoja_ruta_competitor_category).
     * Con @Modifying el DELETE se emite como SQL inmediato antes del INSERT del save().
     */
    @Modifying
    @Query("DELETE FROM HojaRutaMejora h WHERE h.competitor.id = :competitorId AND h.category.id = :categoryId")
    void deleteByCompetitorIdAndCategoryId(@Param("competitorId") Long competitorId,
                                           @Param("categoryId") Long categoryId);

    @Modifying
    @Query("DELETE FROM HojaRutaMejora h WHERE h.competitor.id = :competitorId AND h.category IS NULL")
    void deleteByCompetitorIdAndCategoryIsNull(@Param("competitorId") Long competitorId);
}
