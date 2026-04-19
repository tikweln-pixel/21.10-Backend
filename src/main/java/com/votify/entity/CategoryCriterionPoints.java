package com.votify.entity;

import jakarta.persistence.*;

/**
 * Configuración de puntos máximos por criterio para una categoría concreta.
 */
@Entity
@Table(
    name = "category_criterion_points",
    uniqueConstraints = @UniqueConstraint(columnNames = {"category_id", "criterion_id"})
)
public class CategoryCriterionPoints {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criterion_id", nullable = false)
    private Criterion criterion;

    /** Puntos máximos que se pueden asignar a este criterio en esta categoría. */
    @Column(name = "max_points", nullable = false)
    private Integer weightPercent;

    public CategoryCriterionPoints() {
    }

    public CategoryCriterionPoints(Category category, Criterion criterion, Integer weightPercent) {
        this.category = category;
        this.criterion = criterion;
        this.weightPercent = weightPercent;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Criterion getCriterion() {
        return criterion;
    }

    public void setCriterion(Criterion criterion) {
        this.criterion = criterion;
    }

    public Integer getWeightPercent() {
        return weightPercent;
    }

    public void setWeightPercent(Integer weightPercent) {
        this.weightPercent = weightPercent;
    }
}
