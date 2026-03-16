package com.votify.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    //Tipo de votación de la categoría:
     //  JURY_EXPERT  → Votacion_Jurado_Exp 
     //  POPULAR_VOTE → Voto_Popular       
     
    @Enumerated(EnumType.STRING)
    @Column(name = "voting_type")
    private VotingType votingType;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "time_initial")
    private Date timeInitial;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "time_final")
    private Date timeFinal;

    @Column(name = "reminder_minutes")
    private Integer reminderMinutes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    /** Puntos configurados por criterio para esta categoría (Req. 4 – Configurar Puntos) */
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CategoryCriterionPoints> criterionPoints = new ArrayList<>();

    public Category() {
    }

    public Category(String name, Event event) {
        this.name = name;
        this.event = event;
    }

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

    public Integer getReminderMinutes() {
        return reminderMinutes;
    }

    public void setReminderMinutes(Integer reminderMinutes) {
        this.reminderMinutes = reminderMinutes;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public VotingType getVotingType() {
        return votingType;
    }

    public void setVotingType(VotingType votingType) {
        this.votingType = votingType;
    }

    public List<CategoryCriterionPoints> getCriterionPoints() {
        return criterionPoints;
    }

    public void setCriterionPoints(List<CategoryCriterionPoints> criterionPoints) {
        this.criterionPoints = criterionPoints;
    }
}
