package com.votify.dto;

public class ParticipantDto extends UserDto {

    public ParticipantDto() {
    }

    public ParticipantDto(Long id, String name, String email) {
        super(id, name, email);
    }
}
