package com.votify.dto;

/**
 * Request body for registering an existing user as a competitor in an event.
 * Example: POST /api/events/1/competitors with body { "userId": 5, "categoryId": 2 }
 */
public class RegisterCompetitorRequest {

    private Long userId;
    private Long categoryId;

    public RegisterCompetitorRequest() {
    }

    public RegisterCompetitorRequest(Long userId, Long categoryId) {
        this.userId = userId;
        this.categoryId = categoryId;
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
}
