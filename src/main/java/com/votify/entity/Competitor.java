package com.votify.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "competitors")
@PrimaryKeyJoinColumn(name = "user_id")
public class Competitor extends User {

    public Competitor() {
    }

    public Competitor(String name, String email) {
        super(name, email);
    }

    //Permite a un usuario crear un proyecto para un evento concreto
    public Project createProjectForEvent(String name, String description, Event event) {
        Project project = new Project();
        project.setName(name);
        project.setDescription(description);
        project.setEvent(event);
        project.getCompetitors().add(this);
        return project;
    }
}
