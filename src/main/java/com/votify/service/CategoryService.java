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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

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

    public List<CategoryDto> findAll() {
        List<Category> categories = categoryRepository.findAll();
        List<CategoryDto> result = new ArrayList<>();
        for (Category category : categories) {
            result.add(toDto(category));
        }
        return result;
    }

    public List<CategoryDto> findByEventId(Long eventId) {
        List<Category> categories = categoryRepository.findByEventId(eventId);
        List<CategoryDto> result = new ArrayList<>();
        for (Category category : categories) {
            result.add(toDto(category));
        }
        return result;
    }

    public CategoryDto findById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con id: " + id));
        return toDto(category);
    }

    //metodo no funcional actual. 
    public CategoryDto createForEvent(Long eventId, CategoryDto dto) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con id: " + eventId));
        Category category = new Category(dto.getName(), event);
        category.setVotingType(dto.getVotingType());
        return toDto(categoryRepository.save(category));
    }
    
    public CategoryDto create(CategoryDto dto) {
        if (dto.getEventId() == null) {
            throw new RuntimeException("Se requiere el evento para crear una categoría");
        }
        Event event = eventRepository.findById(dto.getEventId())
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con id: " + dto.getEventId()));

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
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con id: " + id));

        if (dto.getEventId() != null && (category.getEvent() == null || !dto.getEventId().equals(category.getEvent().getId()))) {
            Event event = eventRepository.findById(dto.getEventId())
                    .orElseThrow(() -> new RuntimeException("Evento no encontrado con id: " + dto.getEventId()));
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

    @Transactional
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new RuntimeException("Categoría no encontrada con id: " + id);
        }
        votingRepository.deleteByCategoryId(id);
        eventParticipationRepository.deleteByCategoryId(id);
        criterionPointsRepository.deleteByCategoryId(id);
        categoryRepository.deleteById(id);
    }

    public CategoryDto setVotingType(Long categoryId, VotingType votingType) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con id: " + categoryId));
        category.setVotingType(votingType);
        return toDto(categoryRepository.save(category));
    }

    public List<CategoryCriterionPointsDto> getCriterionPoints(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new RuntimeException("Categoría no encontrada con id: " + categoryId);
        }
        List<CategoryCriterionPoints> points = criterionPointsRepository.findByCategoryId(categoryId);
        List<CategoryCriterionPointsDto> result = new ArrayList<>();
        for (CategoryCriterionPoints ccp : points) {
            result.add(toCriterionPointsDto(ccp));
        }
        return result;
    }

    @Transactional
    public CategoryCriterionPointsDto setCriterionPoints(Long categoryId, Long criterionId, Integer weightPercent) {
        if (weightPercent == null || weightPercent < 0) {
            throw new RuntimeException("El porcentaje de peso debe ser un entero no negativo");
        }

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con id: " + categoryId));

        Criterion criterion = criterionRepository.findById(criterionId)
                .orElseThrow(() -> new RuntimeException("Criterio no encontrado con id: " + criterionId));

        Optional<CategoryCriterionPoints> existing =
                criterionPointsRepository.findByCategoryIdAndCriterionId(categoryId, criterionId);

        int otherPointsSum = 0;
        List<CategoryCriterionPoints> allPoints = criterionPointsRepository.findByCategoryId(categoryId);
        for (CategoryCriterionPoints ccp : allPoints) {
            if (!ccp.getCriterion().getId().equals(criterionId)) {
                otherPointsSum += ccp.getWeightPercent();
            }
        }

        if (otherPointsSum + weightPercent > 100) {
            throw new RuntimeException(
                    "El total de porcentajes de todos los criterios no puede superar 100. Suma actual de otros criterios: "
                    + otherPointsSum + ", valor solicitado: " + weightPercent);
        }

        CategoryCriterionPoints points = existing.orElseGet(() -> new CategoryCriterionPoints(category, criterion, weightPercent));
        points.setWeightPercent(weightPercent);

        return toCriterionPointsDto(criterionPointsRepository.save(points));
    }

    @Transactional
    public List<CategoryCriterionPointsDto> setCriterionPointsBulk(Long categoryId,
                                                                    List<CategoryCriterionPointsDto> pointsDtos) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con id: " + categoryId));

        if (category.getVotingType() == VotingType.POPULAR_VOTE) {
            throw new RuntimeException(
                    "La configuración masiva de puntos solo es válida para categorías JURY_EXPERT. " +
                    "Para POPULAR_VOTE, usa setTotalPoints para configurar los puntos totales de la categoría.");
        }

        for (CategoryCriterionPointsDto dto : pointsDtos) {
            if (dto.getWeightPercent() == null || dto.getWeightPercent() < 0) {
                throw new RuntimeException("El porcentaje de peso debe ser un entero no negativo para el criterio: " + dto.getCriterionId());
            }
        }

        int totalPoints = 0;
        for (CategoryCriterionPointsDto dto : pointsDtos) {
            totalPoints += dto.getWeightPercent();
        }
        if (totalPoints != 100) {
            throw new RuntimeException(
                    "La suma de porcentajes de todos los criterios debe ser exactamente 100. Suma actual: " + totalPoints);
        }

        criterionPointsRepository.deleteByCategoryId(categoryId);

        List<CategoryCriterionPointsDto> result = new ArrayList<>();
        for (CategoryCriterionPointsDto dto : pointsDtos) {
            Criterion criterion = criterionRepository.findById(dto.getCriterionId())
                    .orElseThrow(() -> new RuntimeException("Criterio no encontrado con id: " + dto.getCriterionId()));
            CategoryCriterionPoints saved = criterionPointsRepository.save(
                    new CategoryCriterionPoints(category, criterion, dto.getWeightPercent()));
            result.add(toCriterionPointsDto(saved));
        }
        return result;
    }

    @Transactional
    public void deleteCriterionPoints(Long categoryId, Long criterionId) {
        CategoryCriterionPoints points = criterionPointsRepository
                .findByCategoryIdAndCriterionId(categoryId, criterionId)
                .orElseThrow(() -> new RuntimeException(
                        "No se encontró configuración de puntos para categoryId=" + categoryId + " criterionId=" + criterionId));
        criterionPointsRepository.delete(points);
    }

    @Transactional
    public CategoryDto setTotalPoints(Long categoryId, Integer totalPoints) {
        if (totalPoints == null || totalPoints <= 0) {
            throw new RuntimeException("Los puntos totales deben ser un entero positivo");
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con id: " + categoryId));

        if (category.getVotingType() != VotingType.POPULAR_VOTE) {
            throw new RuntimeException(
                    "setTotalPoints solo es válido para categorías POPULAR_VOTE. " +
                    "Para JURY_EXPERT, usa setCriterionPointsBulk para configurar los pesos por criterio.");
        }
        category.setTotalPoints(totalPoints);
        return toDto(categoryRepository.save(category));
    }

    public CategoryDto getTotalPoints(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con id: " + categoryId));
        return toDto(category);
    }

    @Transactional
    public CategoryDto setMaxVotesPerVoter(Long categoryId, Integer maxVotesPerVoter) {
        if (maxVotesPerVoter == null || maxVotesPerVoter <= 0) {
            throw new RuntimeException("El máximo de votos por votante debe ser un entero positivo");
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con id: " + categoryId));

        if (category.getVotingType() != VotingType.POPULAR_VOTE) {
            throw new RuntimeException("setMaxVotesPerVoter solo es válido para categorías POPULAR_VOTE.");
        }
        category.setMaxVotesPerVoter(maxVotesPerVoter);
        return toDto(categoryRepository.save(category));
    }

    public CategoryDto setTimeInitial(Long id, Date timeInitial) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con id: " + id));
        validateCategoryTimesWithinEvent(category.getEvent(), timeInitial, category.getTimeFinal());
        category.setTimeInitial(timeInitial);
        return toDto(categoryRepository.save(category));
    }

    public CategoryDto setTimeFinal(Long id, Date timeFinal) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con id: " + id));
        validateCategoryTimesWithinEvent(category.getEvent(), category.getTimeInitial(), timeFinal);
        category.setTimeFinal(timeFinal);
        return toDto(categoryRepository.save(category));
    }

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
            throw new RuntimeException("La fecha de inicio de la categoría no puede ser anterior a la del evento");
        }
        if (end != null && evEnd != null && end.after(evEnd)) {
            throw new RuntimeException("La fecha de fin de la categoría no puede ser posterior a la del evento");
        }
        if (start != null && end != null && end.before(start)) {
            throw new RuntimeException("La fecha de fin de la categoría no puede ser anterior a su fecha de inicio");
        }
    }
}
