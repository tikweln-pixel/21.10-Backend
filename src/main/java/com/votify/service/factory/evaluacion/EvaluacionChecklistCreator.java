package com.votify.service.factory.evaluacion;

import com.votify.dto.EvaluacionDto;
import com.votify.entity.Evaluacion;
import com.votify.entity.EvaluacionChecklist;
import com.votify.entity.TipoEvaluacion;

/**
 * Creador concreto — crea instancias de EvaluacionChecklist.
 */
public class EvaluacionChecklistCreator extends EvaluacionCreator {

    @Override
    public Evaluacion create(EvaluacionDto dto) {
        EvaluacionChecklist evaluacion = new EvaluacionChecklist();
        evaluacion.setPeso(dto.getPeso());
        evaluacion.setDatos(dto.getDatos());
        return evaluacion;
    }

    @Override
    public TipoEvaluacion getTipo() {
        return TipoEvaluacion.CHECKLIST;
    }
}
