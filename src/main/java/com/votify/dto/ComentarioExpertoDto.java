package com.votify.dto;

/**
 * Representa un único comentario de experto dentro de un área de mejora.
 */
public class ComentarioExpertoDto {

    private Long evaluadorId;
    private String evaluadorNombre;
    private String texto;

    public ComentarioExpertoDto() {
    }

    public ComentarioExpertoDto(Long evaluadorId, String evaluadorNombre, String texto) {
        this.evaluadorId = evaluadorId;
        this.evaluadorNombre = evaluadorNombre;
        this.texto = texto;
    }

    public Long getEvaluadorId() { return evaluadorId; }
    public void setEvaluadorId(Long evaluadorId) { this.evaluadorId = evaluadorId; }

    public String getEvaluadorNombre() { return evaluadorNombre; }
    public void setEvaluadorNombre(String evaluadorNombre) { this.evaluadorNombre = evaluadorNombre; }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
}
