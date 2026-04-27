package com.votify.dto;

public class RegisterEventUserRequest {

    private Long userId;

    public RegisterEventUserRequest() {
    }

    public RegisterEventUserRequest(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
