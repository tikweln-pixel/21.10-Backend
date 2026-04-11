package com.votify.dto;

import java.util.Date;

public class EvaluacionDto {

    private Long id;
    private String tipo;
    private Long evaluadorId;
    private Long competitorId;
    private Long categoryId;
    private Long criterionId;
    private Double peso;
    private String datos;
    private Double score;
    private Date createdAt;

    public EvaluacionDto() {
    }

    public EvaluacionDto(Long id, String tipo, Long evaluadorId, Long competitorId,
                         Long categoryId, Long criterionId, Double peso, String datos,
                         Double score, Date createdAt) {
        this.id = id;
        this.tipo = tipo;
        this.evaluadorId = evaluadorId;
        this.competitorId = competitorId;
        this.categoryId = categoryId;
        this.criterionId = criterionId;
        this.peso = peso;
        this.datos = datos;
        this.score = score;
        this.createdAt = createdAt;
    }

    public EvaluacionDto(String tipo, Long evaluadorId, Long competitorId,
                         Long categoryId, Long criterionId, Double peso, String datos) {
        this.tipo = tipo;
        this.evaluadorId = evaluadorId;
        this.competitorId = competitorId;
        this.categoryId = categoryId;
        this.criterionId = criterionId;
        this.peso = peso;
        this.datos = datos;
    }

    // Getters y Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Long getEvaluadorId() {
        return evaluadorId;
    }

    public void setEvaluadorId(Long evaluadorId) {
        this.evaluadorId = evaluadorId;
    }

    public Long getCompetitorId() {
        return competitorId;
    }

    public void setCompetitorId(Long competitorId) {
        this.competitorId = competitorId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getCriterionId() {
        return criterionId;
    }

    public void setCriterionId(Long criterionId) {
        this.criterionId = criterionId;
    }

    public Double getPeso() {
        return peso;
    }

    public void setPeso(Double peso) {
        this.peso = peso;
    }

    public String getDatos() {
        return datos;
    }

    public void setDatos(String datos) {
        this.datos = datos;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
