package com.votify.dto;

public class VotingDto {

    private Long id;
    private Long voterId;
    private Long competitorId;
    private Long criterionId;
    private Integer score;

    public VotingDto() {
    }

    public VotingDto(Long id, Long voterId, Long competitorId, Long criterionId, Integer score) {
        this.id = id;
        this.voterId = voterId;
        this.competitorId = competitorId;
        this.criterionId = criterionId;
        this.score = score;
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
}
