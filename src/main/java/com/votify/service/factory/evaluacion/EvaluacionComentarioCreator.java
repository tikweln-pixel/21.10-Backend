package com.votify.service.factory.evaluacion;

import com.votify.dto.EvaluacionDto;
import com.votify.entity.Evaluacion;
import com.votify.entity.EvaluacionComentario;
import com.votify.entity.TipoEvaluacion;

/**
 * Creador concreto — crea instancias de EvaluacionComentario.
 */
public class EvaluacionComentarioCreator extends EvaluacionCreator {

    @Override
    public Evaluacion create(EvaluacionDto dto) {
        EvaluacionComentario evaluacion = new EvaluacionComentario();
        evaluacion.setPeso(dto.getPeso());
        evaluacion.setDatos(dto.getDatos());
        return evaluacion;
    }

    @Override
    public TipoEvaluacion getTipo() {
        return TipoEvaluacion.COMENTARIO;
    }
}
