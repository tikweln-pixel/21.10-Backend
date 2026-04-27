package com.votify.dto;

import java.util.List;

/**
 * Agrupa los comentarios de expertos recibidos para un criterio concreto.
 * Representa una "área de mejora" en la hoja de ruta del competidor.
 */
public class AreaMejoraDto {

    private Long criterioId;
    private String criterioNombre;
    private List<ComentarioExpertoDto> comentarios;

    public AreaMejoraDto() {
    }

    public AreaMejoraDto(Long criterioId, String criterioNombre, List<ComentarioExpertoDto> comentarios) {
        this.criterioId = criterioId;
        this.criterioNombre = criterioNombre;
        this.comentarios = comentarios;
    }

    public Long getCriterioId() { return criterioId; }
    public void setCriterioId(Long criterioId) { this.criterioId = criterioId; }

    public String getCriterioNombre() { return criterioNombre; }
    public void setCriterioNombre(String criterioNombre) { this.criterioNombre = criterioNombre; }

    public List<ComentarioExpertoDto> getComentarios() { return comentarios; }
    public void setComentarios(List<ComentarioExpertoDto> comentarios) { this.comentarios = comentarios; }
}
