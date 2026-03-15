package com.votify.dto;

import com.votify.entity.ParticipationRole;

/**
 * Request for registering a user as competitor or voter in a category of an event.
 */
public class RegisterParticipationRequest {

    private Long userId;
    private Long categoryId;
    private ParticipationRole role;

    public RegisterParticipationRequest() {
    }

    public RegisterParticipationRequest(Long userId, Long categoryId, ParticipationRole role) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.role = role;
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
}
