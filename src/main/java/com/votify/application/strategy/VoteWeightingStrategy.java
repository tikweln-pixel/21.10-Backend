package com.votify.application.strategy;

import com.votify.entity.Category;
import com.votify.entity.Voting;

/**
 * Interfaz para estrategias de ponderación de votos.
 * Implementaciones deben ser beans Spring y registrarse con una key conocida.
 */
public interface VoteWeightingStrategy {

	/**
	 * Identificador de la estrategia. Usado para selección en tiempo de ejecución.
	 */
	String key();

	double applyWeight(Voting vote, Category category);
}
