package com.votify.mapper;

import com.votify.dto.CategoryDto;
import com.votify.entity.Category;
import org.springframework.stereotype.Component;

/**
 * Mapper para convertir entre Category (entidad) y CategoryDto (DTO).
 * Centraliza la lógica de transformación y elimina duplicación en servicios.
 */
@Component
public class CategoryMapper {

	/**
	 * Convierte una entidad Category a CategoryDto
	 */
	public CategoryDto toDto(Category category) {
		if (category == null) {
			return null;
		}

		Long eventId = category.getEvent() != null ? category.getEvent().getId() : null;

		CategoryDto dto = new CategoryDto(
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

		return dto;
	}

	/**
	 * Convierte un DTO a una entidad Category (parcialmente, para actualización)
	 */
	public void updateFromDto(CategoryDto dto, Category category) {
		if (dto == null || category == null) {
			return;
		}

		if (dto.getName() != null && !dto.getName().isEmpty()) {
			category.setName(dto.getName());
		}

		if (dto.getVotingType() != null) {
			category.setVotingType(dto.getVotingType());
		}

		if (dto.getTimeInitial() != null) {
			category.setTimeInitial(dto.getTimeInitial());
		}

		if (dto.getTimeFinal() != null) {
			category.setTimeFinal(dto.getTimeFinal());
		}

		if (dto.getMaxVotesPerVoter() != null) {
			category.setMaxVotesPerVoter(dto.getMaxVotesPerVoter());
		}

		if (dto.getTotalPoints() != null) {
			category.setTotalPoints(dto.getTotalPoints());
		}
	}
}
