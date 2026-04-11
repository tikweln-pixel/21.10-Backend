package com.votify.service;

import com.votify.dto.EvaluacionDto;
import com.votify.entity.*;
import com.votify.persistence.*;
import com.votify.service.factory.evaluacion.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Client del patrón Factory Method (GoF) — ADR-006.
 *
 * Usa un Map&lt;TipoEvaluacion, EvaluacionCreator&gt; para seleccionar el Creator
 * concreto sin if/else. Al añadir un nuevo tipo de evaluación, solo hay que:
 * 1) Crear el ConcreteProduct (entidad)
 * 2) Crear el ConcreteCreator
 * 3) Añadir una entrada al Map
 */
@SuppressWarnings("null")
@Service
public class EvaluacionService {

    private final EvaluacionRepository evaluacionRepository;
    private final UserRepository userRepository;
    private final CompetitorRepository competitorRepository;
    private final CategoryRepository categoryRepository;
    private final CriterionRepository criterionRepository;

    private final Map<TipoEvaluacion, EvaluacionCreator> creators;

    public EvaluacionService(EvaluacionRepository evaluacionRepository,
                             UserRepository userRepository,
                             CompetitorRepository competitorRepository,
                             CategoryRepository categoryRepository,
                             CriterionRepository criterionRepository) {
        this.evaluacionRepository = evaluacionRepository;
        this.userRepository = userRepository;
        this.competitorRepository = competitorRepository;
        this.categoryRepository = categoryRepository;
        this.criterionRepository = criterionRepository;

        // Registro de creators — el único punto donde se enumeran los tipos.
        // Añadir un tipo nuevo = 1 línea aquí + 2 clases nuevas (Product + Creator).
        this.creators = Map.of(
                TipoEvaluacion.NUMERICA,    new EvaluacionNumericaCreator(),
                TipoEvaluacion.CHECKLIST,   new EvaluacionChecklistCreator(),
                TipoEvaluacion.RUBRICA,     new EvaluacionRubricaCreator(),
                TipoEvaluacion.COMENTARIO,  new EvaluacionComentarioCreator(),
                TipoEvaluacion.AUDIO,       new EvaluacionAudioCreator(),
                TipoEvaluacion.VIDEO,       new EvaluacionVideoCreator()
        );
    }

    // CRUD

    public List<EvaluacionDto> findAll() {
        return evaluacionRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public EvaluacionDto findById(Long id) {
        Evaluacion evaluacion = evaluacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evaluacion not found with id: " + id));
        return toDto(evaluacion);
    }

    @Transactional
    public EvaluacionDto create(EvaluacionDto dto) {
        // 1. Parsear tipo y obtener creator (sin if/else)
        TipoEvaluacion tipo;
        try {
            tipo = TipoEvaluacion.valueOf(dto.getTipo());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Tipo de evaluación no válido: " + dto.getTipo());
        }

        EvaluacionCreator creator = creators.get(tipo);
        if (creator == null) {
            throw new RuntimeException("No hay creator registrado para el tipo: " + tipo);
        }

        // 2. Factory Method — crea la instancia correcta con validación
        Evaluacion evaluacion = creator.createAndValidate(dto);

        // 3. Resolver relaciones vía repositorios
        User evaluador = userRepository.findById(dto.getEvaluadorId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + dto.getEvaluadorId()));
        evaluacion.setEvaluador(evaluador);

        Competitor competitor = competitorRepository.findById(dto.getCompetitorId())
                .orElseThrow(() -> new RuntimeException("Competitor not found with id: " + dto.getCompetitorId()));
        evaluacion.setCompetitor(competitor);

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + dto.getCategoryId()));
        evaluacion.setCategory(category);

        if (dto.getCriterionId() != null) {
            Criterion criterion = criterionRepository.findById(dto.getCriterionId())
                    .orElseThrow(() -> new RuntimeException("Criterion not found with id: " + dto.getCriterionId()));
            evaluacion.setCriterion(criterion);
        }

        evaluacion.setCreatedAt(new Date());

        // 4. Persistir y devolver con score calculado
        return toDto(evaluacionRepository.save(evaluacion));
    }

    @Transactional
    public EvaluacionDto update(Long id, EvaluacionDto dto) {
        Evaluacion evaluacion = evaluacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evaluacion not found with id: " + id));

        if (dto.getPeso() != null) {
            if (dto.getPeso() < 0) {
                throw new RuntimeException("El peso de la evaluación no puede ser negativo");
            }
            evaluacion.setPeso(dto.getPeso());
        }
        if (dto.getDatos() != null) {
            evaluacion.setDatos(dto.getDatos());
        }
        if (dto.getEvaluadorId() != null) {
            User evaluador = userRepository.findById(dto.getEvaluadorId())
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + dto.getEvaluadorId()));
            evaluacion.setEvaluador(evaluador);
        }
        if (dto.getCompetitorId() != null) {
            Competitor competitor = competitorRepository.findById(dto.getCompetitorId())
                    .orElseThrow(() -> new RuntimeException("Competitor not found with id: " + dto.getCompetitorId()));
            evaluacion.setCompetitor(competitor);
        }
        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found with id: " + dto.getCategoryId()));
            evaluacion.setCategory(category);
        }
        if (dto.getCriterionId() != null) {
            Criterion criterion = criterionRepository.findById(dto.getCriterionId())
                    .orElseThrow(() -> new RuntimeException("Criterion not found with id: " + dto.getCriterionId()));
            evaluacion.setCriterion(criterion);
        }

        return toDto(evaluacionRepository.save(evaluacion));
    }

    @Transactional
    public void delete(Long id) {
        if (!evaluacionRepository.existsById(id)) {
            throw new RuntimeException("Evaluacion not found with id: " + id);
        }
        evaluacionRepository.deleteById(id);
    }

    // Queries

    public List<EvaluacionDto> findByCategory(Long categoryId) {
        return evaluacionRepository.findByCategoryId(categoryId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<EvaluacionDto> findByCompetitor(Long competitorId) {
        return evaluacionRepository.findByCompetitorId(competitorId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<EvaluacionDto> findByCategoryAndCompetitor(Long categoryId, Long competitorId) {
        return evaluacionRepository.findByCategoryIdAndCompetitorId(categoryId, competitorId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // Helper

    private EvaluacionDto toDto(Evaluacion evaluacion) {
        Long evaluadorId = evaluacion.getEvaluador() != null ? evaluacion.getEvaluador().getId() : null;
        Long competitorId = evaluacion.getCompetitor() != null ? evaluacion.getCompetitor().getId() : null;
        Long categoryId = evaluacion.getCategory() != null ? evaluacion.getCategory().getId() : null;
        Long criterionId = evaluacion.getCriterion() != null ? evaluacion.getCriterion().getId() : null;

        // Obtener el tipo desde el discriminator JPA
        String tipo = evaluacion.getClass().getAnnotation(jakarta.persistence.DiscriminatorValue.class) != null
                ? evaluacion.getClass().getAnnotation(jakarta.persistence.DiscriminatorValue.class).value()
                : "UNKNOWN";

        return new EvaluacionDto(
                evaluacion.getId(),
                tipo,
                evaluadorId,
                competitorId,
                categoryId,
                criterionId,
                evaluacion.getPeso(),
                evaluacion.getDatos(),
                evaluacion.calcularScore(),
                evaluacion.getCreatedAt()
        );
    }
}
