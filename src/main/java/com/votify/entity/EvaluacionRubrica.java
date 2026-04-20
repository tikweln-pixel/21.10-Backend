package com.votify.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * ConcreteProduct — Evaluación tipo rúbrica.
 *
 * Datos esperados: {"niveles": [{"nivel": 3, "max": 5}, {"nivel": 4, "max": 5}]}
 * Score = media de (nivel / max) × 100 para cada criterio de la rúbrica.
 */
@Entity
@DiscriminatorValue("RUBRICA")
public class EvaluacionRubrica extends Evaluacion {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public EvaluacionRubrica() {
    }

    public EvaluacionRubrica(User evaluador, User competitor, Category category,
                             Criterion criterion, Double peso, String datos) {
        super(evaluador, competitor, category, criterion, peso, datos);
    }

    @Override
    public Double calcularScore() {
        try {
            JsonNode root = MAPPER.readTree(getDatos());
            JsonNode niveles = root.get("niveles");
            if (niveles == null || !niveles.isArray() || niveles.size() == 0) {
                return 0.0;
            }
            double sumRatios = 0;
            for (JsonNode nivel : niveles) {
                double n = nivel.get("nivel").asDouble();
                double m = nivel.get("max").asDouble();
                if (m > 0) {
                    sumRatios += (n / m);
                }
            }
            return (sumRatios / niveles.size()) * 100;
        } catch (Exception e) {
            throw new RuntimeException("Error al calcular score rúbrica: " + e.getMessage());
        }
    }
}
