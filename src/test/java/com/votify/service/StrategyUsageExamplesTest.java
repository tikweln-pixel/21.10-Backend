package com.votify.service;

import com.votify.adapters.strategy.CategoryFactorWeightingStrategy;
import com.votify.adapters.strategy.DefaultWeightingStrategy;
import com.votify.application.strategy.VoteWeightingStrategy;
import com.votify.entity.Category;
import com.votify.entity.Voting;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Ejemplos de uso del patrón Strategy — Tests demostrativosTestRepository
 * Muestra casos reales de cómo se usan las estrategias en el código
 */
@DisplayName("Patrón Strategy: Ejemplos de uso real")
class StrategyUsageExamplesTest {

	/**
	 * Ejemplo 1: Usar estrategia por defecto
	 * Caso: Un voto sin ponderación especial
	 */
	@Test
	@DisplayName("Caso real 1: Voto simple sin ponderación")
	void realCase_simpleVoteWithoutWeighting() {
		// Arrange: Tenemos un voto simple
		Voting simpleVote = new Voting();
		simpleVote.setScore(7);

		VoteWeightingStrategy strategy = new DefaultWeightingStrategy();

		// Act: Aplicamos la estrategia
		double weightedScore = strategy.applyWeight(simpleVote, null);

		// Assert: El score se mantiene tal cual
		assertThat(weightedScore).isEqualTo(7.0);
	}

	/**
	 * Ejemplo 2: Usar estrategia con factor de categoría
	 * Caso: Voto en categoría con puntos definidos
	 */
	@Test
	@DisplayName("Caso real 2: Voto con ponderación por puntos de categoría")
	void realCase_voteWithCategoryWeighting() {
		// Arrange: Categoría de diseño con máximo de 100 puntos
		Category designCategory = new Category();
		designCategory.setName("Diseño");
		designCategory.setTotalPoints(100);

		Voting designVote = new Voting();
		designVote.setScore(8); // 8 sobre 10

		VoteWeightingStrategy strategy = new CategoryFactorWeightingStrategy();

		// Act: Aplicamos ponderación
		double weightedScore = strategy.applyWeight(designVote, designCategory);

		// Assert: El score se amplifica: 8.0 * (100 / 10.0) = 80.0
		assertThat(weightedScore).isEqualTo(80.0);
	}

	/**
	 * Ejemplo 3: Selección de estrategia según categoría
	 * Caso: Diferentes categorías requieren diferentes estrategias
	 */
	@Test
	@DisplayName("Caso real 3: Selección dinámica de estrategia")
	void realCase_dynamicStrategySelection() {
		// Arrange: Dos categorías diferentes
		Category simpleCategory = new Category();
		simpleCategory.setName("Votos rápidos");
		simpleCategory.setWeightingPolicy("default");

		Category complexCategory = new Category();
		complexCategory.setName("Votos ponderados");
		complexCategory.setWeightingPolicy("categoryFactor");
		complexCategory.setTotalPoints(100);

		Voting vote = new Voting();
		vote.setScore(6);

		// Act: Dependiendo de la categoría, seleccionamos estrategia
		VoteWeightingStrategy strategyForSimple = simpleCategory.getWeightingPolicy().equals("default") 
			? new DefaultWeightingStrategy() 
			: new CategoryFactorWeightingStrategy();

		VoteWeightingStrategy strategyForComplex = complexCategory.getWeightingPolicy().equals("default")
			? new DefaultWeightingStrategy()
			: new CategoryFactorWeightingStrategy();

		double resultSimple = strategyForSimple.applyWeight(vote, simpleCategory);
		double resultComplex = strategyForComplex.applyWeight(vote, complexCategory);

		// Assert: Resultados diferentes según la estrategia
		assertThat(resultSimple).isEqualTo(6.0);
		assertThat(resultComplex).isEqualTo(60.0); // 6.0 * (100 / 10.0)
		assertThat(resultComplex).isGreaterThan(resultSimple);
	}

	/**
	 * Ejemplo 4: Manejo de categorías sin definición de puntos
	 * Caso: Categoría definida pero sin puntos = usa default
	 */
	@Test
	@DisplayName("Caso real 4: Categoría sin puntos definidos")
	void realCase_categoryWithoutDefinedPoints() {
		// Arrange: Categoría sin puntos (aún no configurada)
		Category undefinedCategory = new Category();
		undefinedCategory.setName("Categoría temporal");
		undefinedCategory.setTotalPoints(null);

		Voting vote = new Voting();
		vote.setScore(5);

		// Strategy: Aunque es categoryFactor, fallback a score base
		VoteWeightingStrategy strategy = new CategoryFactorWeightingStrategy();

		// Act
		double result = strategy.applyWeight(vote, undefinedCategory);

		// Assert: Devuelve el score sin amplificación
		assertThat(result).isEqualTo(5.0);
	}

	/**
	 * Ejemplo 5: Votación con puntuación nula
	 * Caso: Voto no puntuado (score null)
	 */
	@Test
	@DisplayName("Caso real 5: Voto sin puntuación")
	void realCase_nullScore() {
		// Arrange: Voto sin puntuación
		Voting unpunctuatedVote = new Voting();
		unpunctuatedVote.setScore(null);

		Category category = new Category();
		category.setTotalPoints(100);

		// Act: Ambas estrategias manejan null de forma segura
		double resultDefault = new DefaultWeightingStrategy().applyWeight(unpunctuatedVote, category);
		double resultFactor = new CategoryFactorWeightingStrategy().applyWeight(unpunctuatedVote, category);

		// Assert: Ambas devuelven 0.0
		assertThat(resultDefault).isEqualTo(0.0);
		assertThat(resultFactor).isEqualTo(0.0);
	}

	/**
	 * Ejemplo 6: Comparación de dos votos similares con diferentes estrategias
	 * Caso: Competencia donde la ponderación afecta el resultado
	 */
	@Test
	@DisplayName("Caso real 6: Impacto de la estrategia en resultados")
	void realCase_strategyImpactOnResults() {
		// Arrange: Dos evaluadores votan lo mismo pero con estrategias diferentes
		Voting evaluator1Vote = new Voting();
		evaluator1Vote.setScore(8);

		Voting evaluator2Vote = new Voting();
		evaluator2Vote.setScore(8);

		Category categoryWithPoints = new Category();
		categoryWithPoints.setTotalPoints(100);

		// Act: Evaluador 1 usa default, evaluador 2 usa factor
		double score1 = new DefaultWeightingStrategy().applyWeight(evaluator1Vote, categoryWithPoints);
		double score2 = new CategoryFactorWeightingStrategy().applyWeight(evaluator2Vote, categoryWithPoints);

		// Assert: Mismo voto, diferentes pesos
		assertThat(score1).isEqualTo(8.0);
		assertThat(score2).isEqualTo(80.0);

		// El evaluador 2 tiene más influencia por la categoría con puntos
		assertThat(score2).isEqualTo(score1 * 10);
	}

	/**
	 * Ejemplo 7: Estrategia robusta ante valores extremos
	 * Caso: Sistema recibe un valor excepcional
	 */
	@Test
	@DisplayName("Caso real 7: Manejo de valores extremos")
	void realCase_extremeValues() {
		// Arrange: Un voto con puntuación muy alta
		Voting extremeVote = new Voting();
		extremeVote.setScore(999);

		Category largeCategory = new Category();
		largeCategory.setTotalPoints(Integer.MAX_VALUE / 2);

		// Act: Las estrategias deben ser robustas
		double resultDefault = new DefaultWeightingStrategy().applyWeight(extremeVote, largeCategory);
		double resultFactor = new CategoryFactorWeightingStrategy().applyWeight(extremeVote, largeCategory);

		// Assert: Manejo correcto sin overflow
		assertThat(resultDefault).isEqualTo(999.0);
		assertThat(resultFactor).isGreaterThan(resultDefault);
		assertThat(resultFactor).isFinite();
	}

	/**
	 * Ejemplo 8: Usar Strategy pattern para implementar nuevas políticas
	 * Caso: Se necesita una estrategia custom sin cambiar código existente
	 */
	@Test
	@DisplayName("Caso real 8: Extensibilidad — nueva estrategia custom")
	void realCase_customStrategy() {
		// Crear una estrategia custom (ej: weighted average)
		VoteWeightingStrategy customStrategy = new VoteWeightingStrategy() {
			@Override
			public String key() {
				return "weightedAverage";
			}

			@Override
			public double applyWeight(Voting vote, Category category) {
				double baseScore = vote.getScore() != null ? vote.getScore() : 0.0;
				if (category != null && category.getTotalPoints() != null && category.getTotalPoints() > 0) {
					// Custom logic: 50% del score + 50% del factor
					double factor = category.getTotalPoints().doubleValue() / 10.0;
					return (baseScore * 0.5) + (factor * 0.5);
				}
				return baseScore;
			}
		};

		// Arrange
		Voting vote = new Voting();
		vote.setScore(10);

		Category category = new Category();
		category.setTotalPoints(50);

		// Act: Aplicar estrategia custom
		double result = customStrategy.applyWeight(vote, category);

		// Assert: Custom logic funciona sin modificar código existente
		// (10.0 * 0.5) + ((50 / 10.0) * 0.5) = 5.0 + 2.5 = 7.5
		assertThat(result).isEqualTo(7.5);
	}

	/**
	 * Ejemplo 9: Flujo completo de votación
	 * Caso: Pipeline completo desde votación hasta resultado
	 */
	@Test
	@DisplayName("Caso real 9: Flujo completo de votación")
	void realCase_completVotingFlowProcess() {
		// Arrange: Simular una votación completa
		Voting userVote = new Voting();
		userVote.setScore(8);

		Category evaluationCategory = new Category();
		evaluationCategory.setName("Performance");
		evaluationCategory.setTotalPoints(100);
		evaluationCategory.setWeightingPolicy("categoryFactor");

		// Act: Seleccionar e aplicar estrategia basada en categoría
		VoteWeightingStrategy selectedStrategy;
		if ("categoryFactor".equals(evaluationCategory.getWeightingPolicy())) {
			selectedStrategy = new CategoryFactorWeightingStrategy();
		} else {
			selectedStrategy = new DefaultWeightingStrategy();
		}

		double finalScore = selectedStrategy.applyWeight(userVote, evaluationCategory);

		// Assert: Resultado correcto
		assertThat(selectedStrategy.key()).isEqualTo("categoryFactor");
		assertThat(finalScore).isEqualTo(80.0); // 8 * (100 / 10.0)
	}

	/**
	 * Ejemplo 10: Validación de consistencia
	 * Caso: Garantizar que la misma entrada siempre produce la misma salida
	 */
	@Test
	@DisplayName("Caso real 10: Garantía de consistencia")
	void realCase_consistencyGuarantee() {
		// Arrange
		Voting vote = new Voting();
		vote.setScore(5);

		Category category = new Category();
		category.setTotalPoints(40);

		VoteWeightingStrategy strategy = new CategoryFactorWeightingStrategy();

		// Act: Ejecutar múltiples veces
		double result1 = strategy.applyWeight(vote, category);
		double result2 = strategy.applyWeight(vote, category);
		double result3 = strategy.applyWeight(vote, category);

		// Assert: Siempre el mismo resultado
		assertThat(result1).isEqualTo(result2).isEqualTo(result3).isEqualTo(20.0);
	}
}
