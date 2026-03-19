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
import com.votify.persistence.EventRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final CriterionRepository criterionRepository;
    private final CategoryCriterionPointsRepository criterionPointsRepository;

    public CategoryService(CategoryRepository categoryRepository,
                           EventRepository eventRepository,
                           CriterionRepository criterionRepository,
                           CategoryCriterionPointsRepository criterionPointsRepository) {
        this.categoryRepository = categoryRepository;
        this.eventRepository = eventRepository;
        this.criterionRepository = criterionRepository;
        this.criterionPointsRepository = criterionPointsRepository;
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

    public CategoryDto findById(@NonNull Long id) {
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
        category.setVotingType(dto.getVotingType());
        category.setTimeInitial(dto.getTimeInitial());
        category.setTimeFinal(dto.getTimeFinal());
        category.setReminderMinutes(dto.getReminderMinutes());
        return toDto(categoryRepository.save(category));
    }

    public CategoryDto update(Long id, CategoryDto dto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));

        if (dto.getEventId() != null && (category.getEvent() == null || !dto.getEventId().equals(category.getEvent().getId()))) {
            Event event = eventRepository.findById(dto.getEventId())
                    .orElseThrow(() -> new RuntimeException("Event not found with id: " + dto.getEventId()));
            category.setEvent(event);
        }

        Event event = category.getEvent();
        validateCategoryTimesWithinEvent(event, dto.getTimeInitial(), dto.getTimeFinal());

        category.setName(dto.getName());
        category.setVotingType(dto.getVotingType());
        category.setTimeInitial(dto.getTimeInitial());
        category.setTimeFinal(dto.getTimeFinal());
        category.setReminderMinutes(dto.getReminderMinutes());
        return toDto(categoryRepository.save(category));
    }

    public void delete(Long id) {
        categoryRepository.deleteById(id);
    }

    //  Req. 5 – Definir Categorías: tipo de votación                      
    //JURY_EXPERT  → Votacion_Jurado_Exp (diagrama de clases)
    //POPULAR_VOTE → Voto_Popular        (diagrama de clases)

    //Organizador "Elige Categoría" del formulario de creación de evento
    public CategoryDto setVotingType(Long categoryId, VotingType votingType) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));
        category.setVotingType(votingType);
        return toDto(categoryRepository.save(category));
    }

    //  Req. 4 – Configurar Puntos: puntos por criterio por categoría      //
    
   //Devuelve la lista de puntos configurados por criterio para una categoría.
     
    public List<CategoryCriterionPointsDto> getCriterionPoints(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new RuntimeException("Category not found with id: " + categoryId);
        }
        return criterionPointsRepository.findByCategoryId(categoryId).stream()
                .map(this::toCriterionPointsDto)
                .collect(Collectors.toList());
    }

 
     //Crea o actualiza los puntos máximos de un criterio concreto dentro de una categoría.
    //Si ya existía un registro para ese par (categoría, criterio), se actualiza.
     //Si no existía, se crea uno nuevo.
     
     /**
     * @param categoryId  ID de la categoría
     * @param criterionId ID del criterio (Innovación, Calidad Técnica, Presentación…)
     * @param maxPoints   Puntos máximos a asignar (valor del slider en la UI)
     */
    @Transactional
    public CategoryCriterionPointsDto setCriterionPoints(Long categoryId, Long criterionId, Integer maxPoints) {
        if (maxPoints == null || maxPoints < 0) {
            throw new RuntimeException("maxPoints must be a non-negative integer");
        }

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));

        Criterion criterion = criterionRepository.findById(criterionId)
                .orElseThrow(() -> new RuntimeException("Criterion not found with id: " + criterionId));

        Optional<CategoryCriterionPoints> existing =
                criterionPointsRepository.findByCategoryIdAndCriterionId(categoryId, criterionId);

        CategoryCriterionPoints points = existing.orElseGet(() -> new CategoryCriterionPoints(category, criterion, maxPoints));
        points.setMaxPoints(maxPoints);

        return toCriterionPointsDto(criterionPointsRepository.save(points));
    }
     //Reemplaza toda la configuración de puntos de una categoría de una vez.
   
     /**
     * "Configuración de puntos – Puntos Por Categoría".
     *
     * @param categoryId  ID de la categoría
     * @param pointsDtos  Lista de pares (criterionId, maxPoints) a guardar
     */

    @Transactional
    public List<CategoryCriterionPointsDto> setCriterionPointsBulk(Long categoryId,
                                                                    List<CategoryCriterionPointsDto> pointsDtos) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));

        // Eliminamos los registros anteriores y guardamos los nuevos
        criterionPointsRepository.deleteByCategoryId(categoryId);

        List<CategoryCriterionPoints> saved = pointsDtos.stream().map(dto -> {
            if (dto.getMaxPoints() == null || dto.getMaxPoints() < 0) {
                throw new RuntimeException("maxPoints must be a non-negative integer for criterion: " + dto.getCriterionId());
            }
            Criterion criterion = criterionRepository.findById(dto.getCriterionId())
                    .orElseThrow(() -> new RuntimeException("Criterion not found with id: " + dto.getCriterionId()));
            return criterionPointsRepository.save(new CategoryCriterionPoints(category, criterion, dto.getMaxPoints()));
        }).collect(Collectors.toList());

        return saved.stream().map(this::toCriterionPointsDto).collect(Collectors.toList());
    }

    
     // Elimina la configuración de puntos de un criterio concreto en una categoría.
     
    @Transactional
    public void deleteCriterionPoints(Long categoryId, Long criterionId) {
        CategoryCriterionPoints points = criterionPointsRepository
                .findByCategoryIdAndCriterionId(categoryId, criterionId)
                .orElseThrow(() -> new RuntimeException(
                        "No points configuration found for categoryId=" + categoryId + " criterionId=" + criterionId));
        criterionPointsRepository.delete(points);
    }

    //  Período de votación                                      


    public CategoryDto setTimeInitial(Long id, Date timeInitial) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
        validateCategoryTimesWithinEvent(category.getEvent(), timeInitial, category.getTimeFinal());
        category.setTimeInitial(timeInitial);
        return toDto(categoryRepository.save(category));
    }

    public CategoryDto setTimeFinal(Long id, Date timeFinal) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
        validateCategoryTimesWithinEvent(category.getEvent(), category.getTimeInitial(), timeFinal);
        category.setTimeFinal(timeFinal);
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
                category.getReminderMinutes()
        );
    }

    private CategoryCriterionPointsDto toCriterionPointsDto(CategoryCriterionPoints ccp) {
        String criterionName = ccp.getCriterion() != null ? ccp.getCriterion().getName() : null;
        Long categoryId  = ccp.getCategory()  != null ? ccp.getCategory().getId()  : null;
        Long criterionId = ccp.getCriterion() != null ? ccp.getCriterion().getId() : null;
        return new CategoryCriterionPointsDto(ccp.getId(), categoryId, criterionId, criterionName, ccp.getMaxPoints());
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
