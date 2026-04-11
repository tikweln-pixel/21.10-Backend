package com.votify.entity;

/**
 * Tipos de evaluación multicriterio soportados por el sistema.
 * Cada tipo tiene una lógica de cálculo de score distinta (Factory Method — ADR-006).
 */
public enum TipoEvaluacion {
    NUMERICA,
    CHECKLIST,
    RUBRICA,
    COMENTARIO,
    AUDIO,
    VIDEO
}
