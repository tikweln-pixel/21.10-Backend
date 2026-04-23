package com.votify.dto;

public class VotingDto {

    private Long id;
    private Long voterId;
    private Long projectId;
    private Long criterionId;
    private Integer score;

    private Long categoryId;
    private Boolean manuallyModified;
    private String comentario;

    public VotingDto() {
    }

    public VotingDto(Long id, Long voterId, Long projectId, Long criterionId, Integer score) {
        this.id = id;
        this.voterId = voterId;
        this.projectId = projectId;
        this.criterionId = criterionId;
        this.score = score;
    }

    public VotingDto(Long id, Long voterId, Long projectId, Long criterionId, Integer score, Long categoryId) {
        this.id = id;
        this.voterId = voterId;
        this.projectId = projectId;
        this.criterionId = criterionId;
        this.score = score;
        this.categoryId = categoryId;
    }

    public VotingDto(Long id, Long voterId, Long projectId, Long criterionId, Integer score, Long categoryId, Boolean manuallyModified) {
        this.id = id;
        this.voterId = voterId;
        this.projectId = projectId;
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

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
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
