package com.votify.entity;

/**
 * Tipos de evaluación multicriterio soportados por el sistema.
 * Cada tipo tiene una lógica de cálculo de puntuación distinta (Método Fábrica — ADR-006).
 */
public enum TipoEvaluacion {
    NUMERICA,
    CHECKLIST,
    RUBRICA,
    COMENTARIO,
    AUDIO,
    VIDEO
}
