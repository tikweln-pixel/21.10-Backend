package com.votify.adapters.strategy;

import com.votify.application.strategy.VoteWeightingStrategy;
import com.votify.entity.Category;
import com.votify.entity.Voting;
import org.springframework.stereotype.Component;

/**
 * Estrategia por defecto: devuelve el score tal cual.
 */
@Component("defaultWeightingStrategy")
public class DefaultWeightingStrategy implements VoteWeightingStrategy {

	@Override
	public String key() {
		return "default";
	}

	@Override
	public double applyWeight(Voting vote, Category category) {
		return vote.getScore() != null ? vote.getScore() : 0.0;
	}
}
