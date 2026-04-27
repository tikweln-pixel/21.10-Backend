package com.votify.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "votings")
public class Voting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "voter_id", nullable = false)
    private User voter;
    

    @ManyToOne
    @JoinColumn(name = "competitor_id", nullable = false)
    private User competitor;

    @ManyToOne
    @JoinColumn(name = "criterion_id", nullable = false)
    private Criterion criterion;

    /**
     * Req. 19/23 – Categoría a la que pertenece este voto.
     * Obligatorio en POPULAR_VOTE para poder validar las restricciones de puntos y límite de votos.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false)
    private Integer score;

    @Column(name = "manually_modified")
    private Boolean manuallyModified;

    @Column(name = "comentario", columnDefinition = "TEXT")
    private String comentario;

    public Voting() {
    }

    public Voting(User voter, User competitor, Criterion criterion, Integer score) {
        this.voter = voter;
        this.competitor = competitor; 
        this.criterion = criterion;
        this.score = score;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getVoter() {
        return voter;
    }

    public void setVoter(User voter) {
        this.voter = voter;
    }

    public User getCompetitor() {
        return competitor;
    }

    public void setCompetitor(User competitor) {
        this.competitor = competitor;
    }

    public Criterion getCriterion() {
        return criterion;
    }

    public void setCriterion(Criterion criterion) {
        this.criterion = criterion;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Boolean getManuallyModified() {
        return manuallyModified;
    }

    public void setManuallyModified(Boolean manuallyModified) {
        this.manuallyModified = manuallyModified;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }
}

