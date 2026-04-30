package com.votify.adapters.strategy;

import com.votify.application.strategy.VoteWeightingStrategy;
import com.votify.entity.Category;
import com.votify.entity.Voting;
import org.springframework.stereotype.Component;

/**
 * Estrategia que aplica un factor definido en la categoría (totalPoints o un factor específico).
 * Si no hay factor, cae a 1.0.
 */
@Component("categoryFactorWeightingStrategy")
public class CategoryFactorWeightingStrategy implements VoteWeightingStrategy {

	@Override
	public String key() {
		return "categoryFactor";
	}

	@Override
	public double applyWeight(Voting vote, Category category) {
		double base = vote.getScore() != null ? vote.getScore() : 0.0;
		if (category == null) return base;
		Integer totalPoints = category.getTotalPoints();
		if (totalPoints == null || totalPoints == 0) {
			return base;
		}
		// ejemplo simple: normalizar score en rango [0, totalPoints]
		return base * (totalPoints.doubleValue() / 10.0);
	}
}
