package com.votify.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * ConcreteProduct — Evaluación tipo comentario (cualitativa).
 *
 * Datos esperados: {"texto": "Excelente presentación, buen dominio del tema..."}
 * Score = null (no hay puntuación numérica, es puramente cualitativa).
 */
@Entity
@DiscriminatorValue("COMENTARIO")
public class EvaluacionComentario extends Evaluacion {

    public EvaluacionComentario() {
    }

    public EvaluacionComentario(User evaluador, Competitor competitor, Category category,
                                Criterion criterion, Double peso, String datos) {
        super(evaluador, competitor, category, criterion, peso, datos);
    }

    @Override
    public Double calcularScore() {
        return null;
    }
}
