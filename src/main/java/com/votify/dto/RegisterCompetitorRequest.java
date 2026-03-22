package com.votify.dto;

/**
 * Cuerpo de la petición para registrar un usuario existente como competidor en un evento.
 * Ejemplo: POST /api/events/1/competitors con cuerpo { "userId": 5, "categoryId": 2 }
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
