package com.votify.validator;

import com.votify.entity.Voting;
import com.votify.entity.Category;

/**
 * Interfaz Strategy para validar votos según el tipo de votación.
 * Permite diferentes reglas de validación para POPULAR_VOTE vs JURY_EXPERT.
 */
public interface VotingValidator {

	/**
	 * Valida un voto según las reglas específicas del tipo de votación.
	 * Lanza ValidationException si la validación falla.
	 *
	 * @param voting la entidad de voto a validar
	 * @param category la categoría del voto
	 */
	void validateVote(Voting voting, Category category);
}
