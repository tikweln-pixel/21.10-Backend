package com.votify.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.votify.entity.ParticipationRole;

public class EventParticipationDto {

    private Long id;
    private Long eventId;
    private Long userId;
    private Long categoryId;
    private ParticipationRole role;
    private String userName;
    private String userEmail;
    private String categoryName;

    public EventParticipationDto() {
    }

    public EventParticipationDto(Long id, Long eventId, Long userId, Long categoryId, ParticipationRole role) {
        this.id = id;
        this.eventId = eventId;
        this.userId = userId;
        this.categoryId = categoryId;
        this.role = role;
    }

    public EventParticipationDto(Long id, Long eventId, Long userId, Long categoryId, ParticipationRole role,
                                 String userName, String userEmail, String categoryName) {
        this.id = id;
        this.eventId = eventId;
        this.userId = userId;
        this.categoryId = categoryId;
        this.role = role;
        this.userName = userName;
        this.userEmail = userEmail;
        this.categoryName = categoryName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public ParticipationRole getRole() {
        return role;
    }

    public void setRole(ParticipationRole role) {
        this.role = role;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @JsonProperty("email")
    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
}
