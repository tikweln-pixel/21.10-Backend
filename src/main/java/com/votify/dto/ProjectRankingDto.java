package com.votify.dto;

public class ProjectRankingDto {

    private Long projectId;
    private String projectName;
    private Long totalScore;

    public ProjectRankingDto(Long projectId, String projectName, Long totalScore) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.totalScore = totalScore;
    }

    public Long getProjectId() { return projectId; }
    public String getProjectName() { return projectName; }
    public Long getTotalScore() { return totalScore; }
}
