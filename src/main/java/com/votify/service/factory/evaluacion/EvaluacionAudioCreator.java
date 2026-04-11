package com.votify.service.factory.evaluacion;

import com.votify.dto.EvaluacionDto;
import com.votify.entity.Evaluacion;
import com.votify.entity.EvaluacionAudio;
import com.votify.entity.TipoEvaluacion;

/**
 * ConcreteCreator — crea instancias de EvaluacionAudio.
 */
public class EvaluacionAudioCreator extends EvaluacionCreator {

    @Override
    public Evaluacion create(EvaluacionDto dto) {
        EvaluacionAudio evaluacion = new EvaluacionAudio();
        evaluacion.setPeso(dto.getPeso());
        evaluacion.setDatos(dto.getDatos());
        return evaluacion;
    }

    @Override
    public TipoEvaluacion getTipo() {
        return TipoEvaluacion.AUDIO;
    }
}
