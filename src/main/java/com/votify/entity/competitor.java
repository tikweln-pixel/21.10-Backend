package com.votify.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "competitors")
@PrimaryKeyJoinColumn(name = "participant_id")
public class Competitor extends Participant {

    public Competitor() {
    }

    public Competitor(String name, String email) {
        super(name, email);
    }
}
