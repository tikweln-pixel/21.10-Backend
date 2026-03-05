package com.votify.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "participants")
@PrimaryKeyJoinColumn(name = "user_id")
public class Participant extends User {

    public Participant() {
    }

    public Participant(String name, String email) {
        super(name, email);
    }
}
