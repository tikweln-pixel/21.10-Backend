package com.votify;

import com.votify.dto.CategoryCriterionPointsDto;
import com.votify.dto.CategoryDto;
import com.votify.entity.Criterion;
import com.votify.entity.Event;
import com.votify.entity.VotingType;
import com.votify.persistence.CriterionRepository;
import com.votify.persistence.EventRepository;
import com.votify.service.CategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.util.Date;
import java.util.List;

@SpringBootApplication
public class VotifyApplication {

    private static final Logger log = LoggerFactory.getLogger(VotifyApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(VotifyApplication.class, args);
    }

    // ------------------------------------------------------------------ //
    //  TEST DE CATEGORIAS                                                  //
    //  Activar en application.properties:                                 //
    //      votify.test.category=true                                       //
    // ------------------------------------------------------------------ //

    /**
     * Tarea de arranque de prueba para categorías ({@link CommandLineRunner}).
     *
     * Cubre los dos flujos del prototipo:
     *   1. "Crear Nuevo Evento" → lista desplegable "Elige Categoría" ({@link VotingType})
     *   2. "Configuración de puntos" → deslizadores "Puntos por categoría"
     *
     * Escenario:
     *   - Evento:     "Hackathon Votify Test"
     *   - Categorías: "Proyectos Sociales"   (JURY_EXPERT  / Votacion_Jurado_Exp)
     *                 "Proyectos Culturales" (POPULAR_VOTE / Voto_Popular)
     *   - Criterios:  Innovación, Calidad Técnica, Presentación, Viabilidad
     *   - Puntos:     valores distintos por categoría para comprobar que son independientes
     */
    @Bean
    @ConditionalOnProperty(name = "votify.test.category", havingValue = "true")
    public CommandLineRunner categoryTestRunner(
            CategoryService categoryService,
            EventRepository eventRepository,
            CriterionRepository criterionRepository) {

        return args -> {
            log.info("=======================================================");
            log.info("  VOTIFY TEST DE CATEGORIAS");
            log.info("=======================================================");

            // ── 1. Evento de prueba ──────────────────────────────────────
            log.info("[1/5] Creando evento de prueba...");
            Date now   = new Date();
            Date later = new Date(now.getTime() + 7L * 24 * 60 * 60 * 1000); // +7 días

            Event event = new Event("Hackathon Votify Test");
            event.setTimeInitial(now);
            event.setTimeFinal(later);
            event = eventRepository.save(event);
            log.info("  -> Evento creado: id={} nombre='{}'", event.getId(), event.getName());

            // ── 2. Criterios de evaluación ───────────────────────────────
            log.info("[2/5] Creando criterios de evaluación...");
            Criterion cInn = criterionRepository.save(new Criterion("Innovación"));
            Criterion cCal = criterionRepository.save(new Criterion("Calidad Técnica"));
            Criterion cPre = criterionRepository.save(new Criterion("Presentación"));
            Criterion cVia = criterionRepository.save(new Criterion("Viabilidad"));
            log.info("  -> Criterios: {} | {} | {} | {}",
                    cInn.getName(), cCal.getName(), cPre.getName(), cVia.getName());

            // ── 3. Categoría JURY_EXPERT (Votacion_Jurado_Exp) ──────────
            log.info("[3/5] Creando categoría JURY_EXPERT – 'Proyectos Sociales'...");
            CategoryDto dtoJury = new CategoryDto();
            dtoJury.setName("Proyectos Sociales");
            dtoJury.setVotingType(VotingType.JURY_EXPERT);
            dtoJury.setEventId(event.getId());
            dtoJury.setTimeInitial(now);
            dtoJury.setTimeFinal(later);
            CategoryDto catJury = categoryService.create(dtoJury);
            log.info("  -> Categoría creada: id={} nombre='{}' tipo={}",
                    catJury.getId(), catJury.getName(), catJury.getVotingType());

            // Puntos por criterio para JURY_EXPERT (sliders pantalla 2)
            categoryService.setCriterionPoints(catJury.getId(), cInn.getId(), 30);
            categoryService.setCriterionPoints(catJury.getId(), cCal.getId(), 25);
            categoryService.setCriterionPoints(catJury.getId(), cPre.getId(), 25);
            categoryService.setCriterionPoints(catJury.getId(), cVia.getId(), 20);
            log.info("  -> Puntos: Innovación=30 | Calidad=25 | Presentación=25 | Viabilidad=20");

            // ── 4. Categoría POPULAR_VOTE (Voto_Popular) ─────────────────
            log.info("[4/5] Creando categoría POPULAR_VOTE  'Proyectos Culturales'...");
            CategoryDto dtoPop = new CategoryDto();
            dtoPop.setName("Proyectos Culturales");
            dtoPop.setVotingType(VotingType.POPULAR_VOTE);
            dtoPop.setEventId(event.getId());
            dtoPop.setTimeInitial(now);
            dtoPop.setTimeFinal(later);
            CategoryDto catPop = categoryService.create(dtoPop);
            log.info("  -> Categoría creada: id={} nombre='{}' tipo={}",
                    catPop.getId(), catPop.getName(), catPop.getVotingType());

            // Puntos distintos para verificar que cada categoría es independiente
            categoryService.setCriterionPoints(catPop.getId(), cInn.getId(), 40);
            categoryService.setCriterionPoints(catPop.getId(), cCal.getId(), 20);
            categoryService.setCriterionPoints(catPop.getId(), cPre.getId(), 30);
            categoryService.setCriterionPoints(catPop.getId(), cVia.getId(), 10);
            log.info("  -> Puntos: Innovación=40 | Calidad=20 | Presentación=30 | Viabilidad=10");

            // ── 5. Verificación: leer de BBDD y mostrar ──────────────────
            log.info("[5/5] Verificación leyendo categorías del evento id={}...", event.getId());
            List<CategoryDto> categorias = categoryService.findByEventId(event.getId());
            categorias.forEach(cat -> {
                log.info("  ┌─ id={} | '{}' | tipo={}",
                        cat.getId(), cat.getName(), cat.getVotingType());
                List<CategoryCriterionPointsDto> puntos =
                        categoryService.getCriterionPoints(cat.getId());
                puntos.forEach(p ->
                        log.info("  │  criterio='{}' maxPuntos={}", p.getCriterionName(), p.getWeightPercent())
                );
                log.info("  └──────────────────────────────────────");
            });

            log.info("=======================================================");
            log.info("  TEST COMPLETADO CORRECTAMENTE");
            log.info("  Para desactivar: votify.test.category=false");
            log.info("=======================================================");
        };
    }
}
