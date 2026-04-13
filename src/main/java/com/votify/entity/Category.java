package com.votify.entity;

import jakarta.persistence.*;

import java.util.Date;

import static java.util.Objects.requireNonNull;

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

    /**
    POPULAR_VOTE: puntos totales que puede repartir un votante entre todos los competidores.
     */
    @Column(name = "total_points")
    private Integer totalPoints;

    /**
    POPULAR_VOTE: máximo de competidores distintos a los que puede votar un votante.
     */
    @Column(name = "max_votes_per_voter")
    private Integer maxVotesPerVoter;

    protected Category() {}

    public Category(String name, Event event) {
        this.name = requireNonNull(name, "el nombre es obligatorio");
        this.event = event;
    }


    // Cambia el nombre de la categoria
    public void rename(String newName) {
        this.name = requireNonNull(newName, "el nombre es obligatorio");
    }

    // Asigna el tipo de votación (JURY_EXPERT o POPULAR_VOTE)
    public void changeVotingType(VotingType votingType) {
        this.votingType = votingType;
    }

    // Cambia los tiempos de voto en la categoria.
    public void reschedule(Date start, Date end) {
        this.timeInitial = start;
        this.timeFinal = end;
    }

    // Establece solo la fecha/hora de inicio.
    public void changeStartTime(Date start) {
        this.timeInitial = start;
    }

    // Establece solo la fecha/hora de fin
    public void changeEndTime(Date end) {
        this.timeFinal = end;
    }

    // Vincula la categoría a un evento
    public void assignToEvent(Event event) {
        this.event = event;
    }

    // Configura el timepo de recordatorio de las votaciones en minutos
    public void setReminder(Integer minutes) {
        this.reminderMinutes = minutes;
    }

    /** Req. 23 – Configura los puntos totales a repartir en POPULAR_VOTE. */
    public void configureTotalPoints(Integer totalPoints) {
        this.totalPoints = totalPoints;
    }

    /** Req. 19 – Limita el número máximo de competidores a los que puede votar un votante. */
    public void limitVotesPerVoter(Integer maxVotesPerVoter) {
        this.maxVotesPerVoter = maxVotesPerVoter;
    }

    // ─ Getters ─

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public VotingType getVotingType() {
        return votingType;
    }

    public Date getTimeInitial() {
        return timeInitial;
    }

    public Date getTimeFinal() {
        return timeFinal;
    }

    public Event getEvent() {
        return event;
    }

    public Integer getReminderMinutes() {
        return reminderMinutes;
    }

    public Integer getTotalPoints() {
        return totalPoints;
    }

    public Integer getMaxVotesPerVoter() {
        return maxVotesPerVoter;
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
