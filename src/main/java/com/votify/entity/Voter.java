package com.votify.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "voters")
@PrimaryKeyJoinColumn(name = "user_id")
public class Voter extends User {

    public Voter() {
    }

    public Voter(String name, String email, String password) {
        super(name, email, password);
    }
}
