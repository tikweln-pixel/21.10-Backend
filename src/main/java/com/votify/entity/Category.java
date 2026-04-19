package com.votify.entity;

import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "voting_type")
    private VotingType votingType;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "time_initial")
    private Date timeInitial;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "time_final")
    private Date timeFinal;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "event_id")
    private Event event;

    @Column(name = "reminder_minutes")
    private Integer reminderMinutes;

    @Column(name = "total_points")
    private Integer totalPoints;

    @Column(name = "max_votes_per_voter")
    private Integer maxVotesPerVoter;

    public Category() {}

    public Category(String name, Event event) {
        this.name = name;
        this.event = event;
    }

    // ─ Getters y Setters ─

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public VotingType getVotingType() {
        return votingType;
    }

    public void setVotingType(VotingType votingType) {
        this.votingType = votingType;
    }

    public Date getTimeInitial() {
        return timeInitial;
    }

    public void setTimeInitial(Date timeInitial) {
        this.timeInitial = timeInitial;
    }

    public Date getTimeFinal() {
        return timeFinal;
    }

    public void setTimeFinal(Date timeFinal) {
        this.timeFinal = timeFinal;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public Integer getReminderMinutes() {
        return reminderMinutes;
    }

    public void setReminderMinutes(Integer reminderMinutes) {
        this.reminderMinutes = reminderMinutes;
    }

    public Integer getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(Integer totalPoints) {
        this.totalPoints = totalPoints;
    }

    public Integer getMaxVotesPerVoter() {
        return maxVotesPerVoter;
    }

    public void setMaxVotesPerVoter(Integer maxVotesPerVoter) {
        this.maxVotesPerVoter = maxVotesPerVoter;
    }

    // ── Identidad ──

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category)) return false;
        Category other = (Category) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : System.identityHashCode(this);
    }
}
