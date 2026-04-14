package com.votify.service;

import com.votify.dto.CategoryCriterionPointsDto;
import com.votify.dto.CategoryDto;
import com.votify.entity.Category;
import com.votify.entity.CategoryCriterionPoints;
import com.votify.entity.Criterion;
import com.votify.entity.Event;
import com.votify.entity.VotingType;
import com.votify.persistence.CategoryCriterionPointsRepository;
import com.votify.persistence.CategoryRepository;
import com.votify.persistence.CriterionRepository;
import com.votify.persistence.EventParticipationRepository;
import com.votify.persistence.EventRepository;
import com.votify.persistence.VotingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@SuppressWarnings("null")
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final CriterionRepository criterionRepository;
    private final CategoryCriterionPointsRepository criterionPointsRepository;
    private final VotingRepository votingRepository;
    private final EventParticipationRepository eventParticipationRepository;

    public CategoryService(CategoryRepository categoryRepository,
                           EventRepository eventRepository,
                           CriterionRepository criterionRepository,
                           CategoryCriterionPointsRepository criterionPointsRepository,
                           VotingRepository votingRepository,
                           EventParticipationRepository eventParticipationRepository) {
        this.categoryRepository = categoryRepository;
        this.eventRepository = eventRepository;
        this.criterionRepository = criterionRepository;
        this.criterionPointsRepository = criterionPointsRepository;
        this.votingRepository = votingRepository;
        this.eventParticipationRepository = eventParticipationRepository;
    }

    //  CRUD básico


    public List<CategoryDto> findAll() {
        return categoryRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<CategoryDto> findByEventId(Long eventId) {
        return categoryRepository.findByEventId(eventId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public CategoryDto findById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
        return toDto(category);
    }

    public CategoryDto createForEvent(Long eventId, String name) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));
        Category category = new Category(name, event);
        return toDto(categoryRepository.save(category));
    }

    public CategoryDto create(CategoryDto dto) {
        if (dto.getEventId() == null) {
            throw new RuntimeException("Event is required for category creation");
        }
        Event event = eventRepository.findById(dto.getEventId())
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + dto.getEventId()));

        validateCategoryTimesWithinEvent(event, dto.getTimeInitial(), dto.getTimeFinal());

        Category category = new Category(dto.getName(), event);
        category.changeVotingType(dto.getVotingType());
        category.reschedule(dto.getTimeInitial(), dto.getTimeFinal());
        category.setReminder(dto.getReminderMinutes());
        return toDto(categoryRepository.save(category));
    }

    public CategoryDto update(Long id, CategoryDto dto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));

        if (dto.getEventId() != null && (category.getEvent() == null || !dto.getEventId().equals(category.getEvent().getId()))) {
            Event event = eventRepository.findById(dto.getEventId())
                    .orElseThrow(() -> new RuntimeException("Event not found with id: " + dto.getEventId()));
            category.assignToEvent(event);
        }

        Event event = category.getEvent();
        validateCategoryTimesWithinEvent(event, dto.getTimeInitial(), dto.getTimeFinal());

        category.rename(dto.getName());
        category.changeVotingType(dto.getVotingType());
        category.reschedule(dto.getTimeInitial(), dto.getTimeFinal());
        category.setReminder(dto.getReminderMinutes());
        return toDto(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new RuntimeException("Category not found with id: " + id);
        }
        // Delete votings, event participations, criterion points linked to this category
        votingRepository.deleteByCategoryId(id);
        eventParticipationRepository.deleteByCategoryId(id);
        criterionPointsRepository.deleteByCategoryId(id);
        categoryRepository.deleteById(id);
    }

    //  Req. 5 – Definir Categorías: tipo de votación
    //JURY_EXPERT  → Votacion_Jurado_Exp (diagrama de clases)
    //POPULAR_VOTE → Voto_Popular        (diagrama de clases)

    //Organizador "Elige Categoría" del formulario de creación de evento
    public CategoryDto setVotingType(Long categoryId, VotingType votingType) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));
        category.changeVotingType(votingType);
        return toDto(categoryRepository.save(category));
    }

    //  Req. 4 – Configurar Puntos: puntos por criterio por categoría

    //Devuelve la lista de puntos configurados por criterio para una categoría.

    public List<CategoryCriterionPointsDto> getCriterionPoints(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new RuntimeException("Category not found with id: " + categoryId);
        }
        return criterionPointsRepository.findByCategoryId(categoryId).stream()
                .map(this::toCriterionPointsDto)
                .collect(Collectors.toList());
    }

    /**
     * Crea o actualiza los puntos maximos de un criterio concreto dentro de una categoria.
     * Si ya existia un registro para ese par (categoria, criterio), se actualiza.
     * Si no existia, se crea uno nuevo.
     * @param categoryId  ID de la categoria
     * @param criterionId ID del criterio (Innovacion, Calidad Tecnica, Presentacion)
     * @param weightPercent   Puntos maximos a asignar (valor del slider en la UI)
     */
    @Transactional
    public CategoryCriterionPointsDto setCriterionPoints(Long categoryId, Long criterionId, Integer weightPercent) {
        if (weightPercent == null || weightPercent < 0) {
            throw new RuntimeException("weightPercent must be a non-negative integer");
        }

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));

        Criterion criterion = criterionRepository.findById(criterionId)
                .orElseThrow(() -> new RuntimeException("Criterion not found with id: " + criterionId));

        Optional<CategoryCriterionPoints> existing =
                criterionPointsRepository.findByCategoryIdAndCriterionId(categoryId, criterionId);

        // Suma de los weightPercent del resto de criterios (excluyendo el que se edita)
        int otherPointsSum = criterionPointsRepository.findByCategoryId(categoryId).stream()
                .filter(ccp -> !ccp.getCriterion().getId().equals(criterionId))
                .mapToInt(CategoryCriterionPoints::getWeightPercent)
                .sum();

        if (otherPointsSum + weightPercent > 100) {
            throw new RuntimeException(
                    "The total weightPercent for all criteria cannot exceed 100. Current sum of other criteria: "
                    + otherPointsSum + ", attempted value: " + weightPercent);
        }

        CategoryCriterionPoints points = existing.orElseGet(() -> new CategoryCriterionPoints(category, criterion, weightPercent));
        points.setWeightPercent(weightPercent);

        return toCriterionPointsDto(criterionPointsRepository.save(points));
    }

    /**
     * Reemplaza toda la configuracion de puntos de una categoria de una vez.
     * Usado cuando el organizador pulsa "Aceptar" en la pantalla de sliders.
     * Solo aplica a categorías JURY_EXPERT (pesos por criterio que suman 100).
     * @param categoryId  ID de la categoria
     * @param pointsDtos  Lista de pares (criterionId, weightPercent) a guardar
     */
    @Transactional
    public List<CategoryCriterionPointsDto> setCriterionPointsBulk(Long categoryId,
                                                                    List<CategoryCriterionPointsDto> pointsDtos) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));

        if (category.getVotingType() == VotingType.POPULAR_VOTE) {
            throw new RuntimeException(
                    "setCriterionPointsBulk is only valid for JURY_EXPERT categories. " +
                    "For POPULAR_VOTE, use setTotalPoints to configure the total points per category.");
        }

        // Validamos que no haya puntos nulos o negativos antes de hacer la suma
        for (CategoryCriterionPointsDto dto : pointsDtos) {
            if (dto.getWeightPercent() == null || dto.getWeightPercent() < 0) {
                throw new RuntimeException("weightPercent must be a non-negative integer for criterion: " + dto.getCriterionId());
            }
        }

        // Validar que la suma de weightPercent sea exactamente 100
        int totalPoints = pointsDtos.stream()
                .mapToInt(CategoryCriterionPointsDto::getWeightPercent)
                .sum();
        if (totalPoints != 100) {
            throw new RuntimeException(
                    "The sum of weightPercent for all criteria must be exactly 100. Current sum: " + totalPoints);
        }

        // Eliminamos los registros anteriores y guardamos los nuevos
        criterionPointsRepository.deleteByCategoryId(categoryId);

        List<CategoryCriterionPoints> saved = pointsDtos.stream().map(dto -> {
            Criterion criterion = criterionRepository.findById(dto.getCriterionId())
                    .orElseThrow(() -> new RuntimeException("Criterion not found with id: " + dto.getCriterionId()));
            return criterionPointsRepository.save(new CategoryCriterionPoints(category, criterion, dto.getWeightPercent()));
        }).collect(Collectors.toList());

        return saved.stream().map(this::toCriterionPointsDto).collect(Collectors.toList());
    }

    // Elimina la configuracion de puntos de un criterio concreto en una categoria.

    @Transactional
    public void deleteCriterionPoints(Long categoryId, Long criterionId) {
        CategoryCriterionPoints points = criterionPointsRepository
                .findByCategoryIdAndCriterionId(categoryId, criterionId)
                .orElseThrow(() -> new RuntimeException(
                        "No points configuration found for categoryId=" + categoryId + " criterionId=" + criterionId));
        criterionPointsRepository.delete(points);
    }

    //  Req. 23 – Configurar Puntos POPULAR_VOTE

    /**
     * Configura el total de puntos que un votante puede repartir entre los competidores
     * de una categoría de tipo POPULAR_VOTE.
     * Ej: si totalPoints = 10, el votante puede asignar hasta 10 puntos en total.
     * @param categoryId  ID de la categoría POPULAR_VOTE
     * @param totalPoints Total de puntos a repartir (debe ser > 0)
     */
    @Transactional
    public CategoryDto setTotalPoints(Long categoryId, Integer totalPoints) {
        if (totalPoints == null || totalPoints <= 0) {
            throw new RuntimeException("totalPoints must be a positive integer");
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));

        if (category.getVotingType() != VotingType.POPULAR_VOTE) {
            throw new RuntimeException(
                    "setTotalPoints is only valid for POPULAR_VOTE categories. " +
                    "For JURY_EXPERT, use setCriterionPointsBulk to configure weights per criterion.");
        }
        category.configureTotalPoints(totalPoints);
        return toDto(categoryRepository.save(category));
    }

    /**
     * Devuelve el total de puntos configurado para una categoría POPULAR_VOTE.
     */
    public CategoryDto getTotalPoints(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));
        return toDto(category);
    }

    /**
     * Req. 19 – Control de Voto POPULAR_VOTE:
     * Configura el máximo de competidores distintos a los que puede votar un votante.
     * Ej: en un evento con 5 proyectos, el límite es 3.
     * @param categoryId       ID de la categoría POPULAR_VOTE
     * @param maxVotesPerVoter Número máximo de competidores distintos (debe ser > 0)
     */
    @Transactional
    public CategoryDto setMaxVotesPerVoter(Long categoryId, Integer maxVotesPerVoter) {
        if (maxVotesPerVoter == null || maxVotesPerVoter <= 0) {
            throw new RuntimeException("maxVotesPerVoter must be a positive integer");
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));

        if (category.getVotingType() != VotingType.POPULAR_VOTE) {
            throw new RuntimeException(
                    "setMaxVotesPerVoter is only valid for POPULAR_VOTE categories.");
        }
        category.limitVotesPerVoter(maxVotesPerVoter);
        return toDto(categoryRepository.save(category));
    }

    //  Periodo de votacion


    public CategoryDto setTimeInitial(Long id, Date timeInitial) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
        validateCategoryTimesWithinEvent(category.getEvent(), timeInitial, category.getTimeFinal());
        category.changeStartTime(timeInitial);
        return toDto(categoryRepository.save(category));
    }

    public CategoryDto setTimeFinal(Long id, Date timeFinal) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
        validateCategoryTimesWithinEvent(category.getEvent(), category.getTimeInitial(), timeFinal);
        category.changeEndTime(timeFinal);
        return toDto(categoryRepository.save(category));
    }

    //  Helpers

    private CategoryDto toDto(Category category) {
        Long eventId = category.getEvent() != null ? category.getEvent().getId() : null;
        return new CategoryDto(
                category.getId(),
                category.getName(),
                category.getVotingType(),
                category.getTimeInitial(),
                category.getTimeFinal(),
                eventId,
                category.getReminderMinutes(),
                category.getTotalPoints(),
                category.getMaxVotesPerVoter()
        );
    }

    private CategoryCriterionPointsDto toCriterionPointsDto(CategoryCriterionPoints ccp) {
        String criterionName = ccp.getCriterion() != null ? ccp.getCriterion().getName() : null;
        Long categoryId  = ccp.getCategory()  != null ? ccp.getCategory().getId()  : null;
        Long criterionId = ccp.getCriterion() != null ? ccp.getCriterion().getId() : null;
        return new CategoryCriterionPointsDto(ccp.getId(), categoryId, criterionId, criterionName, ccp.getWeightPercent());
    }

    private void validateCategoryTimesWithinEvent(Event event, Date start, Date end) {
        if (event == null) return;

        Date evStart = event.getTimeInitial();
        Date evEnd   = event.getTimeFinal();

        if (start != null && evStart != null && start.before(evStart)) {
            throw new RuntimeException("Category start time cannot be before event start time");
        }
        if (end != null && evEnd != null && end.after(evEnd)) {
            throw new RuntimeException("Category end time cannot be after event end time");
        }
        if (start != null && end != null && end.before(start)) {
            throw new RuntimeException("Category end time cannot be before its start time");
        }
    }
}
