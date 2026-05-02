package com.votify.validator;

import com.votify.entity.Voting;
import com.votify.entity.Category;
import com.votify.entity.CategoryCriterionPoints;
import com.votify.exception.ValidationException;
import com.votify.persistence.CategoryCriterionPointsRepository;

/**
 * Validador para votos JURY_EXPERT.
 * Verifica que el score no exceda los puntos máximos configurados para el criterio.
 */
public class JuryExpertValidator implements VotingValidator {

	private final CategoryCriterionPointsRepository criterionPointsRepository;

	public JuryExpertValidator(CategoryCriterionPointsRepository criterionPointsRepository) {
		this.criterionPointsRepository = criterionPointsRepository;
	}

	@Override
	public void validateVote(Voting voting, Category category) {
		if (voting == null || category == null) {
			return;
		}

		if (criterionPointsRepository == null) {
			throw new ValidationException("criterionPointsRepository",
					"CategoryCriterionPointsRepository no disponible para validar votos JURY_EXPERT");
		}

		Long categoryId = category.getId();
		Long criterionId = voting.getCriterion().getId();
		Integer score = voting.getScore();

		CategoryCriterionPoints points = criterionPointsRepository
				.findByCategoryIdAndCriterionId(categoryId, criterionId)
				.orElseThrow(() -> new ValidationException("criterionPoints",
						String.format(
								"No hay configuración de puntos para el criterio '%s' en la categoría '%s'.",
								voting.getCriterion().getName(),
								category.getName()
						)
				)
		);

		Integer maxPoints = points.getWeightPercent();

		if (score != null && maxPoints != null && score > maxPoints) {
			throw new ValidationException("score",
					String.format(
							"El score (%d) excede los puntos máximos (%d) permitidos para el criterio '%s' " +
							"en la categoría '%s'.",
							score,
							maxPoints,
							voting.getCriterion().getName(),
							category.getName()
					)
			);
		}
	}
}
