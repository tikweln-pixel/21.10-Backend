package com.votify.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * ConcreteProduct — Evaluación numérica.
 *
 * Datos esperados: {"valores": [8, 7, 9]}
 * Score = suma de todos los valores numéricos.
 */
@Entity
@DiscriminatorValue("NUMERICA")
public class EvaluacionNumerica extends Evaluacion {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public EvaluacionNumerica() {
    }

    public EvaluacionNumerica(User evaluador, User competitor, Category category,
                              Criterion criterion, Double peso, String datos) {
        super(evaluador, competitor, category, criterion, peso, datos);
    }

    @Override
    public Double calcularScore() {
        try {
            JsonNode root = MAPPER.readTree(getDatos());
            JsonNode valores = root.get("valores");
            if (valores == null || !valores.isArray()) {
                return 0.0;
            }
            double sum = 0;
            for (JsonNode val : valores) {
                sum += val.asDouble();
            }
            return sum;
        } catch (Exception e) {
            throw new RuntimeException("Error al calcular score numérico: " + e.getMessage());
        }
    }
}
