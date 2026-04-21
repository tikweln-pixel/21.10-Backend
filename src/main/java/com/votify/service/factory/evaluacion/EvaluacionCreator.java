package com.votify.service.factory.evaluacion;

import com.votify.dto.EvaluacionDto;
import com.votify.entity.Evaluacion;
import com.votify.entity.TipoEvaluacion;

/**
 * Creador abstracto del patrón Método Fábrica (GoF) — ADR-006.
 *
 * Declara el método fábrica {@code create()} que cada subclase concreta
 * implementa para decidir qué subtipo de Evaluacion instanciar.
 *
 * La operación {@code createAndValidate()} usa el método fábrica internamente
 * sin conocer el tipo concreto que se va a crear (Método Plantilla).
 */
public abstract class EvaluacionCreator {

    // Método fábrica — las subclases deciden qué tipo concreto crear
    public abstract Evaluacion create(EvaluacionDto dto);

    public abstract TipoEvaluacion getTipo();

    // Método plantilla — valida reglas comunes antes de delegar en create()
    public Evaluacion createAndValidate(EvaluacionDto dto) {
        if (dto.getPeso() != null && dto.getPeso() < 0) {
            throw new RuntimeException("El peso de la evaluación no puede ser negativo");
        }
        return create(dto);
    }
}
