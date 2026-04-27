package com.votify.service.factory.evaluacion;

import com.votify.dto.EvaluacionDto;
import com.votify.entity.Evaluacion;
import com.votify.entity.EvaluacionNumerica;
import com.votify.entity.TipoEvaluacion;

/**
 * Creador concreto — crea instancias de EvaluacionNumerica.
 */
public class EvaluacionNumericaCreator extends EvaluacionCreator {

    @Override
    public Evaluacion create(EvaluacionDto dto) {
        EvaluacionNumerica evaluacion = new EvaluacionNumerica();
        evaluacion.setPeso(dto.getPeso());
        evaluacion.setDatos(dto.getDatos());
        return evaluacion;
    }

    @Override
    public TipoEvaluacion getTipo() {
        return TipoEvaluacion.NUMERICA;
    }
}
