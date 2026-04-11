package com.votify.service.factory.evaluacion;

import com.votify.dto.EvaluacionDto;
import com.votify.entity.Evaluacion;
import com.votify.entity.TipoEvaluacion;

/**
 * Creator abstracto del patrón Factory Method (GoF) — ADR-006.
 *
 * Declara el factory method {@code create()} que cada subclase concreta
 * implementa para decidir qué subtipo de Evaluacion instanciar.
 *
 * La operación {@code createAndValidate()} usa el factory method internamente
 * sin conocer el tipo concreto que se va a crear (Template Method).
 */
public abstract class EvaluacionCreator {

    // Factory Method — las subclases deciden qué tipo concreto crear
    public abstract Evaluacion create(EvaluacionDto dto);

    public abstract TipoEvaluacion getTipo();

    // Template Method — valida reglas comunes antes de delegar en create()
    public Evaluacion createAndValidate(EvaluacionDto dto) {
        if (dto.getPeso() != null && dto.getPeso() < 0) {
            throw new RuntimeException("El peso de la evaluación no puede ser negativo");
        }
        return create(dto);
    }
}
