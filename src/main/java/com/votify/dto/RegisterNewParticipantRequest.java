package com.votify.dto;

public class RegisterNewParticipantRequest {

    private String name;
    private String email;
    private Long categoryId;

    public RegisterNewParticipantRequest() {
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
}
