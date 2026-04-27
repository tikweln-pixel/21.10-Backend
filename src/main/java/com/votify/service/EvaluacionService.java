package com.votify.service;

import com.votify.dto.EvaluacionDto;
import com.votify.entity.*;
import com.votify.persistence.*;
import com.votify.service.factory.evaluacion.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@SuppressWarnings("null")
@Service
public class EvaluacionService {

    private final EvaluacionRepository evaluacionRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final CriterionRepository criterionRepository;

    private final Map<TipoEvaluacion, EvaluacionCreator> creators;

    public EvaluacionService(EvaluacionRepository evaluacionRepository,
                             UserRepository userRepository,
                             CategoryRepository categoryRepository,
                             CriterionRepository criterionRepository) {
        this.evaluacionRepository = evaluacionRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.criterionRepository = criterionRepository;

        this.creators = Map.of(
                TipoEvaluacion.NUMERICA,    new EvaluacionNumericaCreator(),
                TipoEvaluacion.CHECKLIST,   new EvaluacionChecklistCreator(),
                TipoEvaluacion.RUBRICA,     new EvaluacionRubricaCreator(),
                TipoEvaluacion.COMENTARIO,  new EvaluacionComentarioCreator(),
                TipoEvaluacion.AUDIO,       new EvaluacionAudioCreator(),
                TipoEvaluacion.VIDEO,       new EvaluacionVideoCreator()
        );
    }

    public List<EvaluacionDto> findAll() {
        List<Evaluacion> evaluaciones = evaluacionRepository.findAll();
        List<EvaluacionDto> result = new ArrayList<>();
        for (Evaluacion evaluacion : evaluaciones) {
            result.add(toDto(evaluacion));
        }
        return result;
    }

    public EvaluacionDto findById(Long id) {
        Evaluacion evaluacion = evaluacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evaluación no encontrada con id: " + id));
        return toDto(evaluacion);
    }

    @Transactional
    public EvaluacionDto create(EvaluacionDto dto) {
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

        Evaluacion evaluacion = creator.createAndValidate(dto);

        User evaluador = userRepository.findById(dto.getEvaluadorId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + dto.getEvaluadorId()));
        evaluacion.setEvaluador(evaluador);

        User competitor = userRepository.findById(dto.getCompetitorId())
                .orElseThrow(() -> new RuntimeException("Competidor no encontrado con id: " + dto.getCompetitorId()));
        evaluacion.setCompetitor(competitor);

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con id: " + dto.getCategoryId()));
        evaluacion.setCategory(category);

        if (dto.getCriterionId() != null) {
            Criterion criterion = criterionRepository.findById(dto.getCriterionId())
                    .orElseThrow(() -> new RuntimeException("Criterio no encontrado con id: " + dto.getCriterionId()));
            evaluacion.setCriterion(criterion);
        }

        evaluacion.setCreatedAt(new Date());
        return toDto(evaluacionRepository.save(evaluacion));
    }

    @Transactional
    public EvaluacionDto update(Long id, EvaluacionDto dto) {
        Evaluacion evaluacion = evaluacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evaluación no encontrada con id: " + id));

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
            User competitor = userRepository.findById(dto.getCompetitorId())
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
                    .orElseThrow(() -> new RuntimeException("Criterio no encontrado con id: " + dto.getCriterionId()));
            evaluacion.setCriterion(criterion);
        }

        return toDto(evaluacionRepository.save(evaluacion));
    }

    @Transactional
    public void delete(Long id) {
        if (!evaluacionRepository.existsById(id)) {
            throw new RuntimeException("Evaluación no encontrada con id: " + id);
        }
        evaluacionRepository.deleteById(id);
    }

    public List<EvaluacionDto> findByCategory(Long categoryId) {
        List<Evaluacion> evaluaciones = evaluacionRepository.findByCategoryId(categoryId);
        List<EvaluacionDto> result = new ArrayList<>();
        for (Evaluacion evaluacion : evaluaciones) {
            result.add(toDto(evaluacion));
        }
        return result;
    }

    public List<EvaluacionDto> findByCompetitor(Long competitorId) {
        List<Evaluacion> evaluaciones = evaluacionRepository.findByCompetitorId(competitorId);
        List<EvaluacionDto> result = new ArrayList<>();
        for (Evaluacion evaluacion : evaluaciones) {
            result.add(toDto(evaluacion));
        }
        return result;
    }

    public List<EvaluacionDto> findByCategoryAndCompetitor(Long categoryId, Long competitorId) {
        List<Evaluacion> evaluaciones = evaluacionRepository.findByCategoryIdAndCompetitorId(categoryId, competitorId);
        List<EvaluacionDto> result = new ArrayList<>();
        for (Evaluacion evaluacion : evaluaciones) {
            result.add(toDto(evaluacion));
        }
        return result;
    }

    private EvaluacionDto toDto(Evaluacion evaluacion) {
        Long evaluadorId = evaluacion.getEvaluador() != null ? evaluacion.getEvaluador().getId() : null;
        Long competitorId = evaluacion.getCompetitor() != null ? evaluacion.getCompetitor().getId() : null;
        Long categoryId = evaluacion.getCategory() != null ? evaluacion.getCategory().getId() : null;
        Long criterionId = evaluacion.getCriterion() != null ? evaluacion.getCriterion().getId() : null;

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

