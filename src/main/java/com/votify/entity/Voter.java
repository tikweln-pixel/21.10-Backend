package com.votify.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "voters")
@PrimaryKeyJoinColumn(name = "participant_id")
public class Voter extends Participant {

    public Voter() {
    }

    public Voter(String name, String email) {
        super(name, email);
    }
}
