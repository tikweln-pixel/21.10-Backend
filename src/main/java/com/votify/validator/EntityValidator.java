package com.votify.validator;

import com.votify.exception.EntityNotFoundException;
import com.votify.exception.ValidationException;
import com.votify.persistence.*;
import com.votify.entity.*;
import org.springframework.stereotype.Component;

/**
 * Validador centralizado para obtener entidades de la BD y validar valores.
 * Elimina duplicación de código en servicios.
 */
@Component
public class EntityValidator {

	private final UserRepository userRepository;
	private final CategoryRepository categoryRepository;
	private final CriterionRepository criterionRepository;
	private final ProjectRepository projectRepository;
	private final EventRepository eventRepository;

	public EntityValidator(UserRepository userRepository,
						  CategoryRepository categoryRepository,
						  CriterionRepository criterionRepository,
						  ProjectRepository projectRepository,
						  EventRepository eventRepository) {
		this.userRepository = userRepository;
		this.categoryRepository = categoryRepository;
		this.criterionRepository = criterionRepository;
		this.projectRepository = projectRepository;
		this.eventRepository = eventRepository;
	}

	// ══════════════════════════════════════════════════════════════════════════════
	// VALIDACIÓN DE NO NULOS
	// ══════════════════════════════════════════════════════════════════════════════

	public <T> T requireNonNull(T value, String fieldName) {
		if (value == null) {
			throw new ValidationException(fieldName, "El campo no puede ser nulo");
		}
		return value;
	}

	public void validateNonNull(Long value, String fieldName) {
		if (value == null || value <= 0) {
			throw new ValidationException(fieldName, "El ID no puede ser nulo o menor a 1");
		}
	}

	// ══════════════════════════════════════════════════════════════════════════════
	// OBTENCIÓN DE ENTIDADES CON VALIDACIÓN
	// ══════════════════════════════════════════════════════════════════════════════

	public User getUserOrThrow(Long userId) {
		validateNonNull(userId, "userId");
		return userRepository.findById(userId)
				.orElseThrow(() -> new EntityNotFoundException("User", userId));
	}

	public Category getCategoryOrThrow(Long categoryId) {
		validateNonNull(categoryId, "categoryId");
		return categoryRepository.findById(categoryId)
				.orElseThrow(() -> new EntityNotFoundException("Category", categoryId));
	}

	public Criterion getCriterionOrThrow(Long criterionId) {
		validateNonNull(criterionId, "criterionId");
		return criterionRepository.findById(criterionId)
				.orElseThrow(() -> new EntityNotFoundException("Criterion", criterionId));
	}

	public Project getProjectOrThrow(Long projectId) {
		validateNonNull(projectId, "projectId");
		return projectRepository.findById(projectId)
				.orElseThrow(() -> new EntityNotFoundException("Project", projectId));
	}

	public Event getEventOrThrow(Long eventId) {
		validateNonNull(eventId, "eventId");
		return eventRepository.findById(eventId)
				.orElseThrow(() -> new EntityNotFoundException("Event", eventId));
	}

	// ══════════════════════════════════════════════════════════════════════════════
	// VALIDACIONES DE VALORES
	// ══════════════════════════════════════════════════════════════════════════════

	public void validatePercentage(Integer value, String fieldName) {
		if (value == null || value < 0 || value > 100) {
			throw new ValidationException(fieldName, 
					"Debe ser un número entre 0 y 100, recibido: " + value);
		}
	}

	public void validatePositive(Integer value, String fieldName) {
		if (value == null || value <= 0) {
			throw new ValidationException(fieldName, 
					"Debe ser un número positivo, recibido: " + value);
		}
	}

	public void validateNonNegative(Integer value, String fieldName) {
		if (value == null || value < 0) {
			throw new ValidationException(fieldName, 
					"Debe ser un número no negativo, recibido: " + value);
		}
	}

	public void validateStringNotEmpty(String value, String fieldName, int maxLength) {
		if (value == null || value.trim().isEmpty()) {
			throw new ValidationException(fieldName, "El campo no puede estar vacío");
		}
		if (value.length() > maxLength) {
			throw new ValidationException(fieldName, 
					String.format("Máximo %d caracteres, recibido: %d", maxLength, value.length()));
		}
	}

	public void validateDateRange(java.util.Date startDate, java.util.Date endDate, String fieldName) {
		if (startDate != null && endDate != null && endDate.before(startDate)) {
			throw new ValidationException(fieldName, 
					"La fecha de fin no puede ser anterior a la de inicio");
		}
	}

	// ══════════════════════════════════════════════════════════════════════════════
	// VALIDACIONES DE LÓGICA DE NEGOCIO
	// ══════════════════════════════════════════════════════════════════════════════

	public void validateNotSelfVote(Long voterId, Long competitorId) {
		if (voterId.equals(competitorId)) {
			throw new ValidationException("competitor", 
					"No puedes votarte a ti mismo");
		}
	}

	public void validateCategoryVotingType(Category category, VotingType requiredType, String fieldName) {
		if (category.getVotingType() != requiredType) {
			throw new ValidationException(fieldName, 
					String.format("Esta operación solo es válida para categorías %s. " +
							"Categoría actual: %s", requiredType, category.getVotingType()));
		}
	}
}
