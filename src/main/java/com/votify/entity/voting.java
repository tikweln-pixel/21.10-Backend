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
    private Voter voter;

    @ManyToOne
    @JoinColumn(name = "competitor_id", nullable = false)
    private Competitor competitor;

    @ManyToOne
    @JoinColumn(name = "criterion_id", nullable = false)
    private Criterion criterion;

    @Column(nullable = false)
    private Integer score;

    public Voting() {
    }

    public Voting(Voter voter, Competitor competitor, Criterion criterion, Integer score) {
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

    public Voter getVoter() {
        return voter;
    }

    public void setVoter(Voter voter) {
        this.voter = voter;
    }

    public Competitor getCompetitor() {
        return competitor;
    }

    public void setCompetitor(Competitor competitor) {
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
}
