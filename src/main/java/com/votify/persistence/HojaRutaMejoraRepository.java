package com.votify.persistence;

import com.votify.entity.HojaRutaMejora;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HojaRutaMejoraRepository extends JpaRepository<HojaRutaMejora, Long> {

    List<HojaRutaMejora> findByCompetitorId(Long competitorId);

    Optional<HojaRutaMejora> findByCompetitorIdAndCategoryId(Long competitorId, Long categoryId);

    Optional<HojaRutaMejora> findByCompetitorIdAndCategoryIsNull(Long competitorId);

    void deleteByCompetitorIdAndCategoryId(Long competitorId, Long categoryId);

    void deleteByCompetitorIdAndCategoryIsNull(Long competitorId);
}
