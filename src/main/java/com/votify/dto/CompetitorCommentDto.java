package com.votify.dto;

public class CompetitorCommentDto {

    private Long id;
    private String text;
    private Long voterId;
    private Long projectId;
    private String projectName;

    public CompetitorCommentDto() {
    }

    public CompetitorCommentDto(Long id, String text, Long voterId, Long projectId, String projectName) {
        this.id = id;
        this.text = text;
        this.voterId = voterId;
        this.projectId = projectId;
        this.projectName = projectName;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Long getVoterId() { return voterId; }
    public void setVoterId(Long voterId) { this.voterId = voterId; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
}
