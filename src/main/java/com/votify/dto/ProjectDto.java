package com.votify.dto;

import java.util.List;

public class ProjectDto {

    private Long id;
    private String name;
    private String description;
    private Long eventId;
    private List<Long> competitorIds;

    public ProjectDto() {
    }

    public ProjectDto(Long id, String name, String description, Long eventId, List<Long> competitorIds) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.eventId = eventId;
        this.competitorIds = competitorIds;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public List<Long> getCompetitorIds() {
        return competitorIds;
    }

    public void setCompetitorIds(List<Long> competitorIds) {
        this.competitorIds = competitorIds;
    }
}

