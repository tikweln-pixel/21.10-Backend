package com.votify.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * ConcreteProduct — Evaluación tipo vídeo.
 *
 * Datos esperados: {"url": "https://storage.example.com/video.mp4", "scoreManual": 90}
 * Score = scoreManual si está presente, null en caso contrario (pendiente de evaluar).
 */
@Entity
@DiscriminatorValue("VIDEO")
public class EvaluacionVideo extends Evaluacion {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public EvaluacionVideo() {
    }

    public EvaluacionVideo(User evaluador, Competitor competitor, Category category,
                           Criterion criterion, Double peso, String datos) {
        super(evaluador, competitor, category, criterion, peso, datos);
    }

    @Override
    public Double calcularScore() {
        try {
            JsonNode root = MAPPER.readTree(getDatos());
            JsonNode scoreManual = root.get("scoreManual");
            if (scoreManual == null || scoreManual.isNull()) {
                return null;
            }
            return scoreManual.asDouble();
        } catch (Exception e) {
            throw new RuntimeException("Error al calcular score vídeo: " + e.getMessage());
        }
    }
}
