package com.votify.dto;

import java.util.Date;
import java.util.List;

/**
 * DTO completo de la hoja de ruta de mejora de un competidor.
 *
 * - resumenGeneral: párrafo de síntesis automático (o generado por IA en el futuro).
 * - areasMejora: comentarios de expertos agrupados por criterio.
 * - generadoIa: false en la implementación parcial, true cuando se integre IA.
 */
public class HojaRutaMejoraDto {

    private Long id;
    private Long competitorId;
    private Long categoryId;       // null si es global (todas las categorías)
    private String resumenGeneral;
    private List<AreaMejoraDto> areasMejora;
    private boolean generadoIa;
    private Date fechaGeneracion;

    public HojaRutaMejoraDto() {
    }

    public HojaRutaMejoraDto(Long id, Long competitorId, Long categoryId,
                              String resumenGeneral, List<AreaMejoraDto> areasMejora,
                              boolean generadoIa, Date fechaGeneracion) {
        this.id = id;
        this.competitorId = competitorId;
        this.categoryId = categoryId;
        this.resumenGeneral = resumenGeneral;
        this.areasMejora = areasMejora;
        this.generadoIa = generadoIa;
        this.fechaGeneracion = fechaGeneracion;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCompetitorId() { return competitorId; }
    public void setCompetitorId(Long competitorId) { this.competitorId = competitorId; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getResumenGeneral() { return resumenGeneral; }
    public void setResumenGeneral(String resumenGeneral) { this.resumenGeneral = resumenGeneral; }

    public List<AreaMejoraDto> getAreasMejora() { return areasMejora; }
    public void setAreasMejora(List<AreaMejoraDto> areasMejora) { this.areasMejora = areasMejora; }

    public boolean isGeneradoIa() { return generadoIa; }
    public void setGeneradoIa(boolean generadoIa) { this.generadoIa = generadoIa; }

    public Date getFechaGeneracion() { return fechaGeneracion; }
    public void setFechaGeneracion(Date fechaGeneracion) { this.fechaGeneracion = fechaGeneracion; }
}
