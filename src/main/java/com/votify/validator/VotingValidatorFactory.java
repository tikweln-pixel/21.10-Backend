package com.votify.validator;

import com.votify.entity.Category;
import com.votify.entity.VotingType;
import com.votify.exception.BusinessException;
import com.votify.persistence.CategoryCriterionPointsRepository;
import com.votify.persistence.VotingRepository;
import org.springframework.stereotype.Component;

/**
 * Factory para crear el validador apropiado según el tipo de votación de la categoría.
 * Centraliza la lógica de selección de validadores.
 */
@Component
public class VotingValidatorFactory {

	private final VotingRepository votingRepository;
	private final CategoryCriterionPointsRepository criterionPointsRepository;

	public VotingValidatorFactory(VotingRepository votingRepository,
								  CategoryCriterionPointsRepository criterionPointsRepository) {
		this.votingRepository = votingRepository;
		this.criterionPointsRepository = criterionPointsRepository;
	}

	/**
	 * Obtiene el validador apropiado para el tipo de votación de la categoría.
	 *
	 * @param category la categoría que define el tipo de votación
	 * @return el validador apropiado (PopularVoteValidator o JuryExpertValidator)
	 * @throws BusinessException si el tipo de votación no es soportado
	 */
	public VotingValidator getValidator(Category category) {
		if (category == null || category.getVotingType() == null) {
			throw new BusinessException("votingType",
					"La categoría debe tener un tipo de votación definido");
		}

		if (category.getVotingType() == VotingType.POPULAR_VOTE) {
			return new PopularVoteValidator(votingRepository);
		} else if (category.getVotingType() == VotingType.JURY_EXPERT) {
			return new JuryExpertValidator(criterionPointsRepository);
		} else {
			throw new BusinessException("votingType",
					String.format("Tipo de votación no soportado: %s", category.getVotingType()));
		}
	}
}
