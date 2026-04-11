package com.votify.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * ConcreteProduct — Evaluación tipo audio.
 *
 * Datos esperados: {"url": "https://storage.example.com/audio.mp3", "scoreManual": 85}
 * Score = scoreManual si está presente, null en caso contrario (pendiente de evaluar).
 */
@Entity
@DiscriminatorValue("AUDIO")
public class EvaluacionAudio extends Evaluacion {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public EvaluacionAudio() {
    }

    public EvaluacionAudio(User evaluador, Competitor competitor, Category category,
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
            throw new RuntimeException("Error al calcular score audio: " + e.getMessage());
        }
    }
}
