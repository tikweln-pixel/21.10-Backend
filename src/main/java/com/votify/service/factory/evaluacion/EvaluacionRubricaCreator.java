package com.votify.service.factory.evaluacion;

import com.votify.dto.EvaluacionDto;
import com.votify.entity.Evaluacion;
import com.votify.entity.EvaluacionRubrica;
import com.votify.entity.TipoEvaluacion;

/**
 * ConcreteCreator — crea instancias de EvaluacionRubrica.
 */
public class EvaluacionRubricaCreator extends EvaluacionCreator {

    @Override
    public Evaluacion create(EvaluacionDto dto) {
        EvaluacionRubrica evaluacion = new EvaluacionRubrica();
        evaluacion.setPeso(dto.getPeso());
        evaluacion.setDatos(dto.getDatos());
        return evaluacion;
    }

    @Override
    public TipoEvaluacion getTipo() {
        return TipoEvaluacion.RUBRICA;
    }
}
