package com.votify.dto;

public class VotingDto {

    private Long id;
    private Long voterId;
    private Long competitorId;
    private Long criterionId;
    private Integer score;

    /**
     * Req. 19/23 – ID de la categoría a la que pertenece este voto.
     * Requerido para POPULAR_VOTE (validación de límite de votos y puntos totales).
     * Opcional en JURY_EXPERT para mantener compatibilidad.
     */
    private Long categoryId;
    private Boolean manuallyModified;
    private String comentario;

    public VotingDto() {
    }

    public VotingDto(Long id, Long voterId, Long competitorId, Long criterionId, Integer score) {
        this.id = id;
        this.voterId = voterId;
        this.competitorId = competitorId;
        this.criterionId = criterionId;
        this.score = score;
    }

    public VotingDto(Long id, Long voterId, Long competitorId, Long criterionId, Integer score, Long categoryId) {
        this.id = id;
        this.voterId = voterId;
        this.competitorId = competitorId;
        this.criterionId = criterionId;
        this.score = score;
        this.categoryId = categoryId;
    }

    public VotingDto(Long id, Long voterId, Long competitorId, Long criterionId, Integer score, Long categoryId, Boolean manuallyModified) {
        this.id = id;
        this.voterId = voterId;
        this.competitorId = competitorId;
        this.criterionId = criterionId;
        this.score = score;
        this.categoryId = categoryId;
        this.manuallyModified = manuallyModified;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVoterId() {
        return voterId;
    }

    public void setVoterId(Long voterId) {
        this.voterId = voterId;
    }

    public Long getCompetitorId() {
        return competitorId;
    }

    public void setCompetitorId(Long competitorId) {
        this.competitorId = competitorId;
    }

    public Long getCriterionId() {
        return criterionId;
    }

    public void setCriterionId(Long criterionId) {
        this.criterionId = criterionId;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Boolean getManuallyModified() {
        return manuallyModified;
    }

    public void setManuallyModified(Boolean manuallyModified) {
        this.manuallyModified = manuallyModified;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }
}
