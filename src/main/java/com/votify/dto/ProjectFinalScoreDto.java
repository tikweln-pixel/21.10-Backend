package com.votify.dto;

import java.util.List;

/**
 * Nota final ponderada de un proyecto en una categoría JURY_EXPERT.
 * finalScore = suma de (score_criterio_i) donde cada score va de 0 a weightPercent_i.
 * Como los weightPercents suman 100, finalScore ∈ [0, 100].
 */
public class ProjectFinalScoreDto {

    private Long projectId;
    private String projectName;
    private Long categoryId;
    private Integer finalScore;
    private Integer maxScore;
    private List<CriterionScoreDetail> details;

    public ProjectFinalScoreDto() {}

    public ProjectFinalScoreDto(Long projectId, String projectName, Long categoryId,
                                Integer finalScore, Integer maxScore,
                                List<CriterionScoreDetail> details) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.categoryId = categoryId;
        this.finalScore = finalScore;
        this.maxScore = maxScore;
        this.details = details;
    }

    public Long getProjectId()      { return projectId; }
    public String getProjectName()  { return projectName; }
    public Long getCategoryId()     { return categoryId; }
    public Integer getFinalScore()  { return finalScore; }
    public Integer getMaxScore()    { return maxScore; }
    public List<CriterionScoreDetail> getDetails() { return details; }

    public void setProjectId(Long projectId)        { this.projectId = projectId; }
    public void setProjectName(String projectName)  { this.projectName = projectName; }
    public void setCategoryId(Long categoryId)      { this.categoryId = categoryId; }
    public void setFinalScore(Integer finalScore)   { this.finalScore = finalScore; }
    public void setMaxScore(Integer maxScore)       { this.maxScore = maxScore; }
    public void setDetails(List<CriterionScoreDetail> details) { this.details = details; }

    public static class CriterionScoreDetail {
        private Long criterionId;
        private String criterionName;
        private Integer score;
        private Integer weightPercent;

        public CriterionScoreDetail() {}

        public CriterionScoreDetail(Long criterionId, String criterionName,
                                    Integer score, Integer weightPercent) {
            this.criterionId = criterionId;
            this.criterionName = criterionName;
            this.score = score;
            this.weightPercent = weightPercent;
        }

        public Long getCriterionId()     { return criterionId; }
        public String getCriterionName() { return criterionName; }
        public Integer getScore()        { return score; }
        public Integer getWeightPercent(){ return weightPercent; }

        public void setCriterionId(Long criterionId)       { this.criterionId = criterionId; }
        public void setCriterionName(String criterionName) { this.criterionName = criterionName; }
        public void setScore(Integer score)                 { this.score = score; }
        public void setWeightPercent(Integer weightPercent){ this.weightPercent = weightPercent; }
    }
}
