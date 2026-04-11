package com.votify.service.factory.evaluacion;

import com.votify.dto.EvaluacionDto;
import com.votify.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("EvaluacionCreator — Tests unitarios del Factory Method")
class EvaluacionCreatorTest {

    // ── Cada ConcreteCreator produce el subtipo correcto ──────────────

    @Test
    @DisplayName("EvaluacionNumericaCreator → crea EvaluacionNumerica")
    void numericaCreator_createsCorrectType() {
        EvaluacionCreator creator = new EvaluacionNumericaCreator();
        EvaluacionDto dto = new EvaluacionDto("NUMERICA", 1L, 2L, 3L, null, 1.0, "{\"valores\":[8,7]}");

        Evaluacion result = creator.create(dto);

        assertThat(result).isInstanceOf(EvaluacionNumerica.class);
        assertThat(creator.getTipo()).isEqualTo(TipoEvaluacion.NUMERICA);
    }

    @Test
    @DisplayName("EvaluacionChecklistCreator → crea EvaluacionChecklist")
    void checklistCreator_createsCorrectType() {
        EvaluacionCreator creator = new EvaluacionChecklistCreator();
        EvaluacionDto dto = new EvaluacionDto("CHECKLIST", 1L, 2L, 3L, null, 1.0, "{\"items\":[true,false]}");

        Evaluacion result = creator.create(dto);

        assertThat(result).isInstanceOf(EvaluacionChecklist.class);
        assertThat(creator.getTipo()).isEqualTo(TipoEvaluacion.CHECKLIST);
    }

    @Test
    @DisplayName("EvaluacionRubricaCreator → crea EvaluacionRubrica")
    void rubricaCreator_createsCorrectType() {
        EvaluacionCreator creator = new EvaluacionRubricaCreator();
        EvaluacionDto dto = new EvaluacionDto("RUBRICA", 1L, 2L, 3L, null, 1.0, "{\"niveles\":[{\"nivel\":3,\"max\":5}]}");

        Evaluacion result = creator.create(dto);

        assertThat(result).isInstanceOf(EvaluacionRubrica.class);
        assertThat(creator.getTipo()).isEqualTo(TipoEvaluacion.RUBRICA);
    }

    @Test
    @DisplayName("EvaluacionComentarioCreator → crea EvaluacionComentario")
    void comentarioCreator_createsCorrectType() {
        EvaluacionCreator creator = new EvaluacionComentarioCreator();
        EvaluacionDto dto = new EvaluacionDto("COMENTARIO", 1L, 2L, 3L, null, 0.0, "{\"texto\":\"Buen trabajo\"}");

        Evaluacion result = creator.create(dto);

        assertThat(result).isInstanceOf(EvaluacionComentario.class);
        assertThat(creator.getTipo()).isEqualTo(TipoEvaluacion.COMENTARIO);
    }

    @Test
    @DisplayName("EvaluacionAudioCreator → crea EvaluacionAudio")
    void audioCreator_createsCorrectType() {
        EvaluacionCreator creator = new EvaluacionAudioCreator();
        EvaluacionDto dto = new EvaluacionDto("AUDIO", 1L, 2L, 3L, null, 1.0, "{\"url\":\"audio.mp3\"}");

        Evaluacion result = creator.create(dto);

        assertThat(result).isInstanceOf(EvaluacionAudio.class);
        assertThat(creator.getTipo()).isEqualTo(TipoEvaluacion.AUDIO);
    }

    @Test
    @DisplayName("EvaluacionVideoCreator → crea EvaluacionVideo")
    void videoCreator_createsCorrectType() {
        EvaluacionCreator creator = new EvaluacionVideoCreator();
        EvaluacionDto dto = new EvaluacionDto("VIDEO", 1L, 2L, 3L, null, 1.0, "{\"url\":\"video.mp4\"}");

        Evaluacion result = creator.create(dto);

        assertThat(result).isInstanceOf(EvaluacionVideo.class);
        assertThat(creator.getTipo()).isEqualTo(TipoEvaluacion.VIDEO);
    }

    // ── createAndValidate — validación de peso ────────────────────────

    @Test
    @DisplayName("createAndValidate → rechaza peso negativo")
    void createAndValidate_rejectNegativePeso() {
        EvaluacionCreator creator = new EvaluacionNumericaCreator();
        EvaluacionDto dto = new EvaluacionDto("NUMERICA", 1L, 2L, 3L, null, -5.0, "{\"valores\":[1]}");

        assertThatThrownBy(() -> creator.createAndValidate(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("negativo");
    }

    @Test
    @DisplayName("createAndValidate → acepta peso cero")
    void createAndValidate_acceptsZeroPeso() {
        EvaluacionCreator creator = new EvaluacionNumericaCreator();
        EvaluacionDto dto = new EvaluacionDto("NUMERICA", 1L, 2L, 3L, null, 0.0, "{\"valores\":[1]}");

        Evaluacion result = creator.createAndValidate(dto);

        assertThat(result).isInstanceOf(EvaluacionNumerica.class);
        assertThat(result.getPeso()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("createAndValidate → acepta peso positivo")
    void createAndValidate_acceptsPositivePeso() {
        EvaluacionCreator creator = new EvaluacionChecklistCreator();
        EvaluacionDto dto = new EvaluacionDto("CHECKLIST", 1L, 2L, 3L, null, 25.0, "{\"items\":[true]}");

        Evaluacion result = creator.createAndValidate(dto);

        assertThat(result).isInstanceOf(EvaluacionChecklist.class);
        assertThat(result.getPeso()).isEqualTo(25.0);
    }

    @Test
    @DisplayName("createAndValidate → acepta peso null (no obligatorio)")
    void createAndValidate_acceptsNullPeso() {
        EvaluacionCreator creator = new EvaluacionComentarioCreator();
        EvaluacionDto dto = new EvaluacionDto("COMENTARIO", 1L, 2L, 3L, null, null, "{\"texto\":\"ok\"}");

        Evaluacion result = creator.createAndValidate(dto);

        assertThat(result).isInstanceOf(EvaluacionComentario.class);
    }

    // ── calcularScore — lógica específica por tipo ────────────────────

    @Test
    @DisplayName("EvaluacionNumerica.calcularScore → suma valores")
    void numerica_calcularScore_sumsValues() {
        EvaluacionNumerica eval = new EvaluacionNumerica();
        eval.setDatos("{\"valores\": [8, 7, 9]}");

        assertThat(eval.calcularScore()).isEqualTo(24.0);
    }

    @Test
    @DisplayName("EvaluacionChecklist.calcularScore → porcentaje de checked")
    void checklist_calcularScore_percentageChecked() {
        EvaluacionChecklist eval = new EvaluacionChecklist();
        eval.setDatos("{\"items\": [true, false, true, true]}");

        assertThat(eval.calcularScore()).isEqualTo(75.0);
    }

    @Test
    @DisplayName("EvaluacionRubrica.calcularScore → media ponderada × 100")
    void rubrica_calcularScore_weightedAverage() {
        EvaluacionRubrica eval = new EvaluacionRubrica();
        eval.setDatos("{\"niveles\": [{\"nivel\": 4, \"max\": 5}, {\"nivel\": 3, \"max\": 5}]}");

        // (4/5 + 3/5) / 2 * 100 = (0.8 + 0.6) / 2 * 100 = 70.0
        assertThat(eval.calcularScore()).isEqualTo(70.0);
    }

    @Test
    @DisplayName("EvaluacionComentario.calcularScore → null (cualitativa)")
    void comentario_calcularScore_returnsNull() {
        EvaluacionComentario eval = new EvaluacionComentario();
        eval.setDatos("{\"texto\": \"Excelente trabajo\"}");

        assertThat(eval.calcularScore()).isNull();
    }

    @Test
    @DisplayName("EvaluacionAudio.calcularScore → scoreManual presente")
    void audio_calcularScore_withManualScore() {
        EvaluacionAudio eval = new EvaluacionAudio();
        eval.setDatos("{\"url\": \"audio.mp3\", \"scoreManual\": 85}");

        assertThat(eval.calcularScore()).isEqualTo(85.0);
    }

    @Test
    @DisplayName("EvaluacionAudio.calcularScore → sin scoreManual retorna null")
    void audio_calcularScore_withoutManualScore() {
        EvaluacionAudio eval = new EvaluacionAudio();
        eval.setDatos("{\"url\": \"audio.mp3\", \"scoreManual\": null}");

        assertThat(eval.calcularScore()).isNull();
    }

    @Test
    @DisplayName("EvaluacionVideo.calcularScore → scoreManual presente")
    void video_calcularScore_withManualScore() {
        EvaluacionVideo eval = new EvaluacionVideo();
        eval.setDatos("{\"url\": \"video.mp4\", \"scoreManual\": 90}");

        assertThat(eval.calcularScore()).isEqualTo(90.0);
    }
}
