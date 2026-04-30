package com.votify.dto;

/**
 * Cuerpo de la petición para registrar un espectador anónimo en un evento y categoría.
 * Ejemplo: POST /api/events/1/spectators/register
 * con cuerpo { "name": "Espectador Anónimo", "email": "anon_123@votify.local", "categoryId": 2 }
 */
public class RegisterAnonymousSpectatorRequest {

    private String name;
    private String email;
    private Long categoryId;

    public RegisterAnonymousSpectatorRequest() {
    }

    public RegisterAnonymousSpectatorRequest(String name, String email, Long categoryId) {
        this.name = name;
        this.email = email;
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }
}
