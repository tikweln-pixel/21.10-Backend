package com.votify.service.observer;

import com.votify.entity.Category;
import com.votify.entity.Voting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Patrón Observador — Tests unitarios del mecanismo Observer")
class VotingObserverTest {

    // ── Implementación mínima de VotoSubject para los tests ───────────

    static class SubjectDeTest implements VotoSubject {
        private final List<VotoObserver> observers = new ArrayList<>();

        @Override
        public void addObserver(VotoObserver observer) { observers.add(observer); }

        @Override
        public void removeObserver(VotoObserver observer) { observers.remove(observer); }

        @Override
        public void notifyObservers(Voting voting) {
            observers.forEach(o -> o.onVotoGuardado(voting));
        }

        int totalObservers() { return observers.size(); }
    }

    // ── Observer espía: registra cuántas veces fue notificado y con qué voto ──

    static class ObserverEspia implements VotoObserver {
        int vecesNotificado = 0;
        Voting ultimoVoto = null;

        @Override
        public void onVotoGuardado(Voting voting) {
            vecesNotificado++;
            ultimoVoto = voting;
        }
    }

    private SubjectDeTest subject;
    private Voting voto;

    @BeforeEach
    void setUp() {
        subject = new SubjectDeTest();
        voto = new Voting();
        voto.setScore(10);
    }

    // ── addObserver ───────────────────────────────────────────────────

    @Test
    @DisplayName("addObserver → el subject registra un observador correctamente")
    void addObserver_registraObservador() {
        ObserverEspia obs = new ObserverEspia();

        subject.addObserver(obs);

        assertThat(subject.totalObservers()).isEqualTo(1);
    }

    @Test
    @DisplayName("addObserver → se pueden registrar múltiples observadores")
    void addObserver_registraMultiplesObservadores() {
        subject.addObserver(new ObserverEspia());
        subject.addObserver(new ObserverEspia());
        subject.addObserver(new ObserverEspia());

        assertThat(subject.totalObservers()).isEqualTo(3);
    }

    // ── removeObserver ────────────────────────────────────────────────

    @Test
    @DisplayName("removeObserver → el subject elimina el observador registrado")
    void removeObserver_eliminaObservador() {
        ObserverEspia obs = new ObserverEspia();
        subject.addObserver(obs);

        subject.removeObserver(obs);

        assertThat(subject.totalObservers()).isEqualTo(0);
    }

    @Test
    @DisplayName("removeObserver → al eliminar uno, el resto permanece")
    void removeObserver_mantieneLosRestantes() {
        ObserverEspia obs1 = new ObserverEspia();
        ObserverEspia obs2 = new ObserverEspia();
        subject.addObserver(obs1);
        subject.addObserver(obs2);

        subject.removeObserver(obs1);

        assertThat(subject.totalObservers()).isEqualTo(1);
    }

    // ── notifyObservers ───────────────────────────────────────────────

    @Test
    @DisplayName("notifyObservers → el observador recibe exactamente una notificación")
    void notifyObservers_notificaUnaVez() {
        ObserverEspia obs = new ObserverEspia();
        subject.addObserver(obs);

        subject.notifyObservers(voto);

        assertThat(obs.vecesNotificado).isEqualTo(1);
    }

    @Test
    @DisplayName("notifyObservers → el observador recibe el voto correcto")
    void notifyObservers_entregaElVotoCorrecto() {
        ObserverEspia obs = new ObserverEspia();
        subject.addObserver(obs);

        subject.notifyObservers(voto);

        assertThat(obs.ultimoVoto).isSameAs(voto);
        assertThat(obs.ultimoVoto.getScore()).isEqualTo(10);
    }

    @Test
    @DisplayName("notifyObservers → todos los observadores registrados son notificados")
    void notifyObservers_notificaTodosLosObservadores() {
        ObserverEspia obs1 = new ObserverEspia();
        ObserverEspia obs2 = new ObserverEspia();
        ObserverEspia obs3 = new ObserverEspia();
        subject.addObserver(obs1);
        subject.addObserver(obs2);
        subject.addObserver(obs3);

        subject.notifyObservers(voto);

        assertThat(obs1.vecesNotificado).isEqualTo(1);
        assertThat(obs2.vecesNotificado).isEqualTo(1);
        assertThat(obs3.vecesNotificado).isEqualTo(1);
    }

    @Test
    @DisplayName("notifyObservers → observador eliminado no recibe la notificación")
    void notifyObservers_noNotificaObservadorEliminado() {
        ObserverEspia obs = new ObserverEspia();
        subject.addObserver(obs);
        subject.removeObserver(obs);

        subject.notifyObservers(voto);

        assertThat(obs.vecesNotificado).isEqualTo(0);
    }

    @Test
    @DisplayName("notifyObservers → sin observadores no lanza excepción")
    void notifyObservers_sinObservadoresNoFalla() {
        assertThatCode(() -> subject.notifyObservers(voto))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("notifyObservers → múltiples notificaciones acumulan el conteo")
    void notifyObservers_multiplesNotificacionesAcumulan() {
        ObserverEspia obs = new ObserverEspia();
        subject.addObserver(obs);

        subject.notifyObservers(voto);
        subject.notifyObservers(voto);
        subject.notifyObservers(voto);

        assertThat(obs.vecesNotificado).isEqualTo(3);
    }

    // ── RankingObserver — comportamiento con categoría ────────────────

    @Test
    @DisplayName("RankingObserver → no falla si el voto no tiene categoría")
    void rankingObserver_ignoraVotoSinCategoria() {
        RankingObserver obs = new RankingObserver(null, null, null);
        Voting votoSinCategoria = new Voting();
        votoSinCategoria.setScore(5);

        assertThatCode(() -> obs.onVotoGuardado(votoSinCategoria))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("RankingObserver → implementa la interfaz VotoObserver")
    void rankingObserver_implementaInterfaz() {
        RankingObserver obs = new RankingObserver(null, null, null);

        assertThat(obs).isInstanceOf(VotoObserver.class);
    }

    @Test
    @DisplayName("RankingObserver → el voto lleva correctamente la categoría asignada")
    void rankingObserver_usaLaCategoriaDelVoto() {
        Category categoria = new Category();
        categoria.setId(42L);

        Voting votoConCategoria = new Voting();
        votoConCategoria.setScore(8);
        votoConCategoria.setCategory(categoria);

        assertThat(votoConCategoria.getCategory()).isNotNull();
        assertThat(votoConCategoria.getCategory().getId()).isEqualTo(42L);
    }
}
