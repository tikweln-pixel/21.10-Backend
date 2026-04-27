package com.votify.entity;

import jakarta.persistence.*;
import java.util.Date;

/**
 * Producto abstracto del patrón Método Fábrica (GoF) — ADR-006.
 *
 * Clase base para las evaluaciones multicriterio. Cada subtipo concreto
 * implementa {@code calcularScore()} con lógica distinta según el tipo
 * de evaluación (numérica, checklist, rúbrica, comentario, audio, vídeo).
 *
 * Estrategia JPA: SINGLE_TABLE — los 6 subtipos difieren en comportamiento
 * (calcularScore), no en columnas. El campo {@code datos} almacena un JSON
 * con la estructura específica de cada tipo.
 */
@Entity
@Table(name = "evaluaciones")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo", discriminatorType = DiscriminatorType.STRING)
public abstract class Evaluacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluador_id", nullable = false)
    private User evaluador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competitor_id", nullable = false)
    private User competitor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criterion_id")
    private Criterion criterion;

    @Column
    private Double peso;

    @Column(columnDefinition = "TEXT")
    private String datos;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at")
    private Date createdAt;

    /**
     * Método fábrica del Producto — cada subtipo concreto calcula la puntuación
     * de forma distinta según su tipo de evaluación.
     *
     * @return puntuación calculada, o null para tipos cualitativos (COMENTARIO)
     */
    public abstract Double calcularScore();

    // Constructores

    protected Evaluacion() {
    }

    protected Evaluacion(User evaluador, User competitor, Category category,
                         Criterion criterion, Double peso, String datos) {
        this.evaluador = evaluador;
        this.competitor = competitor;
        this.category = category;
        this.criterion = criterion;
        this.peso = peso;
        this.datos = datos;
        this.createdAt = new Date();
    }

    // Getters y Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getEvaluador() {
        return evaluador;
    }

    public void setEvaluador(User evaluador) {
        this.evaluador = evaluador;
    }

    public User getCompetitor() {
        return competitor;
    }

    public void setCompetitor(User competitor) {
        this.competitor = competitor;
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

    public Double getPeso() {
        return peso;
    }

    public void setPeso(Double peso) {
        this.peso = peso;
    }

    public String getDatos() {
        return datos;
    }

    public void setDatos(String datos) {
        this.datos = datos;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
