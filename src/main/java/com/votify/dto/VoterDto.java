package com.votify.dto;

public class VoterDto extends ParticipantDto {

    public VoterDto() {
    }

    public VoterDto(Long id, String name, String email) {
        super(id, name, email);
    }
}
