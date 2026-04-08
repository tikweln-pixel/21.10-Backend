package com.votify.dto;

public class CompetitorDto extends UserDto {

    public CompetitorDto() {
    }

    public CompetitorDto(Long id, String name, String email) {
        super(id, name, email);
    }
}
