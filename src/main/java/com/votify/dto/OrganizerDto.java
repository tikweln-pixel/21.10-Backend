package com.votify.dto;

public class OrganizerDto extends UserDto {

    public OrganizerDto() {
    }

    public OrganizerDto(Long id, String name, String email) {
        super(id, name, email);
    }

    public OrganizerDto(Long id, String name, String email, String password) {
        super(id, name, email, password);
    }
}
