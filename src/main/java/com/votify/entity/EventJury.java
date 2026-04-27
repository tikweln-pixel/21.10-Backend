package com.votify.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "event_jury", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"event_id", "user_id"})
})
public class EventJury {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public EventJury() {}

    public EventJury(Event event, User user) {
        this.event = event;
        this.user = user;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
