package com.votify.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Producto concreto — Evaluación tipo checklist.
 *
 * Datos esperados: {"items": [true, false, true, true]}
 * Score = (número de items marcados / total de items) × 100.
 */
@Entity
@DiscriminatorValue("CHECKLIST")
public class EvaluacionChecklist extends Evaluacion {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public EvaluacionChecklist() {
    }

    public EvaluacionChecklist(User evaluador, User competitor, Category category,
                               Criterion criterion, Double peso, String datos) {
        super(evaluador, competitor, category, criterion, peso, datos);
    }

    @Override
    public Double calcularScore() {
        try {
            JsonNode root = MAPPER.readTree(getDatos());
            JsonNode items = root.get("items");
            if (items == null || !items.isArray() || items.size() == 0) {
                return 0.0;
            }
            int checked = 0;
            for (JsonNode item : items) {
                if (item.asBoolean()) {
                    checked++;
                }
            }
            return (checked * 100.0) / items.size();
        } catch (Exception e) {
            throw new RuntimeException("Error al calcular score checklist: " + e.getMessage());
        }
    }
}
