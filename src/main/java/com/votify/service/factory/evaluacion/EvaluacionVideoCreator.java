package com.votify.service.factory.evaluacion;

import com.votify.dto.EvaluacionDto;
import com.votify.entity.Evaluacion;
import com.votify.entity.EvaluacionVideo;
import com.votify.entity.TipoEvaluacion;

/**
 * ConcreteCreator — crea instancias de EvaluacionVideo.
 */
public class EvaluacionVideoCreator extends EvaluacionCreator {

    @Override
    public Evaluacion create(EvaluacionDto dto) {
        EvaluacionVideo evaluacion = new EvaluacionVideo();
        evaluacion.setPeso(dto.getPeso());
        evaluacion.setDatos(dto.getDatos());
        return evaluacion;
    }

    @Override
    public TipoEvaluacion getTipo() {
        return TipoEvaluacion.VIDEO;
    }
}
