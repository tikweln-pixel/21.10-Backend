package com.votify.validator;

import com.votify.entity.Voting;
import com.votify.entity.Category;
import com.votify.exception.ValidationException;
import com.votify.persistence.VotingRepository;

/**
 * Validador para votos POPULAR_VOTE.
 * Verifica restricciones de cantidad máxima de competidores y puntos totales permitidos.
 */
public class PopularVoteValidator implements VotingValidator {

	private final VotingRepository votingRepository;

	public PopularVoteValidator(VotingRepository votingRepository) {
		this.votingRepository = votingRepository;
	}

	@Override
	public void validateVote(Voting voting, Category category) {
		if (voting == null || category == null) {
			return;
		}

		Long voterId = voting.getVoter().getId();
		Long competitorId = voting.getCompetitor().getId();
		Long categoryId = category.getId();

		// Validar máximo de competidores distintos
		if (category.getMaxVotesPerVoter() != null) {
			long distinctCompetitorsCount = votingRepository
					.countDistinctCompetitorsByVoterIdAndCategoryId(voterId, categoryId);

			boolean isNewCompetitor = isNewVoteForCompetitor(voterId, competitorId, categoryId);

			if (isNewCompetitor && distinctCompetitorsCount >= category.getMaxVotesPerVoter()) {
				throw new ValidationException("maxVotesPerVoter",
						String.format(
								"El votante ya ha alcanzado el límite de %d competidores distintos " +
								"permitidos en la categoría '%s'.",
								category.getMaxVotesPerVoter(),
								category.getName()
						)
				);
			}
		}

		// Validar total de puntos permitidos
		if (category.getTotalPoints() != null && voting.getScore() != null) {
			int alreadyUsedPoints = votingRepository.sumScoreByVoterIdAndCategoryId(voterId, categoryId);
			int requestedPoints = voting.getScore();

			if (alreadyUsedPoints + requestedPoints > category.getTotalPoints()) {
				throw new ValidationException("score",
						String.format(
								"El votante superaría el total de puntos permitidos (%d) en la categoría '%s'. " +
								"Puntos ya usados: %d, puntos solicitados: %d.",
								category.getTotalPoints(),
								category.getName(),
								alreadyUsedPoints,
								requestedPoints
						)
				);
			}
		}
	}

	/**
	 * Verifica si el voto es para un competidor nuevo (no existe voto previo del votante para este competidor)
	 */
	private boolean isNewVoteForCompetitor(Long voterId, Long competitorId, Long categoryId) {
		return votingRepository.findByVoterIdAndCategoryId(voterId, categoryId)
				.stream()
				.noneMatch(v -> v.getCompetitor().getId().equals(competitorId));
	}
}
