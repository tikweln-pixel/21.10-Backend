package com.votify.service;

import com.votify.adapters.strategy.CategoryFactorWeightingStrategy;
import com.votify.adapters.strategy.DefaultWeightingStrategy;
import com.votify.application.strategy.VoteWeightingStrategy;
import com.votify.entity.Category;
import com.votify.entity.Voting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios para el patrón Strategy aplicado a VoteWeightingStrategy.
 * Verifica el comportamiento de cada estrategia de ponderación de votos.
 */
@DisplayName("Patrón Strategy: VoteWeightingStrategy — Tests unitarios")
class VoteWeightingStrategyTest {

	private Voting mockVoting;
	private Category mockCategory;

	private VoteWeightingStrategy defaultStrategy;
	private VoteWeightingStrategy categoryFactorStrategy;

	@BeforeEach
	void setUp() {
		mockVoting = new Voting();
		mockCategory = new Category();

		defaultStrategy = new DefaultWeightingStrategy();
		categoryFactorStrategy = new CategoryFactorWeightingStrategy();
	}

	@Nested
	@DisplayName("DefaultWeightingStrategy — Tests")
	class DefaultWeightingStrategyTests {

		@Test
		@DisplayName("Debe devolver la key 'default'")
		void shouldReturnDefaultKey() {
			String key = defaultStrategy.key();
			assertThat(key).isEqualTo("default");
		}

		@Test
		@DisplayName("Debe devolver el score tal cual cuando el voto tiene score válido")
		void shouldReturnScoreAsIs_whenVotingHasValidScore() {
			// Arrange
			mockVoting.setScore(8);

			// Act
			double result = defaultStrategy.applyWeight(mockVoting, mockCategory);

			// Assert
			assertThat(result).isEqualTo(8.0);
		}

		@Test
		@DisplayName("Debe devolver 0.0 cuando el voto tiene score null")
		void shouldReturn0_whenVotingScoreIsNull() {
			// Arrange
			mockVoting.setScore(null);

			// Act
			double result = defaultStrategy.applyWeight(mockVoting, mockCategory);

			// Assert
			assertThat(result).isEqualTo(0.0);
		}

		@Test
		@DisplayName("Debe devolver 0.0 cuando el voto tiene score cero")
		void shouldReturn0_whenVotingScoreIsZero() {
			// Arrange
			mockVoting.setScore(0);

			// Act
			double result = defaultStrategy.applyWeight(mockVoting, mockCategory);

			// Assert
			assertThat(result).isEqualTo(0.0);
		}

		@Test
		@DisplayName("Debe devolver score negativo si es proporcionado")
		void shouldReturnNegativeScore_whenVotingHasNegativeScore() {
			// Arrange
			mockVoting.setScore(-5);

			// Act
			double result = defaultStrategy.applyWeight(mockVoting, mockCategory);

			// Assert
			assertThat(result).isEqualTo(-5.0);
		}

		@Test
		@DisplayName("Debe ignorar la categoría y devolver el score del voto")
		void shouldIgnoreCategory_andReturnVotingScore() {
			// Arrange
			mockVoting.setScore(7);
			mockCategory.setTotalPoints(100);

			// Act
			double result = defaultStrategy.applyWeight(mockVoting, mockCategory);

			// Assert
			assertThat(result).isEqualTo(7.0);
		}

		@Test
		@DisplayName("Debe ignorar category null y devolver el score del voto")
		void shouldIgnoreNullCategory_andReturnVotingScore() {
			// Arrange
			mockVoting.setScore(6);

			// Act
			double result = defaultStrategy.applyWeight(mockVoting, null);

			// Assert
			assertThat(result).isEqualTo(6.0);
		}

		@Test
		@DisplayName("Debe manejar scores de punto flotante con precisión")
		void shouldHandleFloatingPointScoresWithPrecision() {
			// Arrange
			mockVoting.setScore(3);

			// Act
			double result = defaultStrategy.applyWeight(mockVoting, mockCategory);

			// Assert
			assertThat(result).isEqualTo(3.0);
		}

		@Test
		@DisplayName("Debe manejar scores muy altos")
		void shouldHandleVeryHighScores() {
			// Arrange
			mockVoting.setScore(999999);

			// Act
			double result = defaultStrategy.applyWeight(mockVoting, mockCategory);

			// Assert
			assertThat(result).isEqualTo(999999.0);
		}
	}

	@Nested
	@DisplayName("CategoryFactorWeightingStrategy — Tests")
	class CategoryFactorWeightingStrategyTests {

		@Test
		@DisplayName("Debe devolver la key 'categoryFactor'")
		void shouldReturnCategoryFactorKey() {
			String key = categoryFactorStrategy.key();
			assertThat(key).isEqualTo("categoryFactor");
		}

		@Test
		@DisplayName("Debe aplicar el factor de categoría correctamente")
		void shouldApplyWeightingFactorCorrectly() {
			// Arrange
			mockVoting.setScore(10);
			mockCategory.setTotalPoints(50);

			// Act
			double result = categoryFactorStrategy.applyWeight(mockVoting, mockCategory);

			// Assert
			// score * (totalPoints / 10.0) = 10 * (50 / 10.0) = 50.0
			assertThat(result).isEqualTo(50.0);
		}

		@Test
		@DisplayName("Debe devolver solo el score cuando totalPoints es null")
		void shouldReturnScoreOnly_whenTotalPointsIsNull() {
			// Arrange
			mockVoting.setScore(8);
			mockCategory.setTotalPoints(null);

			// Act
			double result = categoryFactorStrategy.applyWeight(mockVoting, mockCategory);

			// Assert
			assertThat(result).isEqualTo(8.0);
		}

		@Test
		@DisplayName("Debe devolver solo el score cuando totalPoints es cero")
		void shouldReturnScoreOnly_whenTotalPointsIsZero() {
			// Arrange
			mockVoting.setScore(8);
			mockCategory.setTotalPoints(0);

			// Act
			double result = categoryFactorStrategy.applyWeight(mockVoting, mockCategory);

			// Assert
			assertThat(result).isEqualTo(8.0);
		}

		@Test
		@DisplayName("Debe devolver 0.0 cuando el score es null")
		void shouldReturn0_whenScoreIsNull() {
			// Arrange
			mockVoting.setScore(null);
			mockCategory.setTotalPoints(50);

			// Act
			double result = categoryFactorStrategy.applyWeight(mockVoting, mockCategory);

			// Assert
			assertThat(result).isEqualTo(0.0);
		}

		@Test
		@DisplayName("Debe devolver 0.0 cuando tanto score como totalPoints son nulos/cero")
		void shouldReturn0_whenScoreAndTotalPointsAreNullOrZero() {
			// Arrange
			mockVoting.setScore(null);
			mockCategory.setTotalPoints(0);

			// Act
			double result = categoryFactorStrategy.applyWeight(mockVoting, mockCategory);

			// Assert
			assertThat(result).isEqualTo(0.0);
		}

		@Test
		@DisplayName("Debe ignorar categoría null y devolver solo el score")
		void shouldIgnoreNullCategory_andReturnVotingScore() {
			// Arrange
			mockVoting.setScore(5);

			// Act
			double result = categoryFactorStrategy.applyWeight(mockVoting, null);

			// Assert
			assertThat(result).isEqualTo(5.0);
		}

		@Test
		@DisplayName("Debe calcular correctamente con puntos de categoría bajos")
		void shouldCalculateCorrectly_withLowCategoryPoints() {
			// Arrange
			mockVoting.setScore(5);
			mockCategory.setTotalPoints(10);

			// Act
			double result = categoryFactorStrategy.applyWeight(mockVoting, mockCategory);

			// Assert
			// 5.0 * (10 / 10.0) = 5.0
			assertThat(result).isEqualTo(5.0);
		}

		@Test
		@DisplayName("Debe calcular correctamente con puntos de categoría altos")
		void shouldCalculateCorrectly_withHighCategoryPoints() {
			// Arrange
			mockVoting.setScore(8);
			mockCategory.setTotalPoints(100);

			// Act
			double result = categoryFactorStrategy.applyWeight(mockVoting, mockCategory);

			// Assert
			// 8.0 * (100 / 10.0) = 80.0
			assertThat(result).isEqualTo(80.0);
		}

		@Test
		@DisplayName("Debe manejar score decimal con puntos enteros")
		void shouldHandle_decimalScoreWithIntegerPoints() {
			// Arrange
			mockVoting.setScore(4);
			mockCategory.setTotalPoints(40);

			// Act
			double result = categoryFactorStrategy.applyWeight(mockVoting, mockCategory);

			// Assert
			// 4 * (40 / 10.0) = 16.0
			assertThat(result).isEqualTo(16.0);
		}

		@Test
		@DisplayName("Debe aplicar factor fraccional correctamente")
		void shouldApplyFractionalFactorCorrectly() {
			// Arrange
			mockVoting.setScore(9);
			mockCategory.setTotalPoints(15);

			// Act
			double result = categoryFactorStrategy.applyWeight(mockVoting, mockCategory);

			// Assert
			// 9.0 * (15 / 10.0) = 13.5
			assertThat(result).isEqualTo(13.5);
		}
	}

	@Nested
	@DisplayName("Comparación de Estrategias — Tests")
	class StrategyComparisonTests {

		@Test
		@DisplayName("DefaultWeightingStrategy y CategoryFactorWeightingStrategy deben devolver keys diferentes")
		void strategiesShouldHaveDifferentKeys() {
			String defaultKey = defaultStrategy.key();
			String categoryFactorKey = categoryFactorStrategy.key();

			assertThat(defaultKey)
					.isNotEqualTo(categoryFactorKey)
					.isEqualTo("default");
			assertThat(categoryFactorKey).isEqualTo("categoryFactor");
		}

		@Test
		@DisplayName("Sin categoría, ambas estrategias deben devolver el mismo resultado")
		void bothStrategies_shouldReturnSameResult_withoutCategory() {
			// Arrange
			mockVoting.setScore(7);

			// Act
			double defaultResult = defaultStrategy.applyWeight(mockVoting, null);
			double categoryFactorResult = categoryFactorStrategy.applyWeight(mockVoting, null);

			// Assert
			assertThat(defaultResult).isEqualTo(categoryFactorResult);
		}

		@Test
		@DisplayName("Con categoría sin totalPoints, ambas deben devolver el mismo score")
		void bothStrategies_shouldReturnSameScore_whenCategoryHasNoTotalPoints() {
			// Arrange
			mockVoting.setScore(6);
			mockCategory.setTotalPoints(null);

			// Act
			double defaultResult = defaultStrategy.applyWeight(mockVoting, mockCategory);
			double categoryFactorResult = categoryFactorStrategy.applyWeight(mockVoting, mockCategory);

			// Assert
			assertThat(defaultResult).isEqualTo(categoryFactorResult);
		}

		@Test
		@DisplayName("Con categoría con totalPoints, CategoryFactor debe amplificar el score")
		void categoryFactorStrategy_shouldAmplifyScore_whenCategoryHasTotalPoints() {
			// Arrange
			mockVoting.setScore(5);
			mockCategory.setTotalPoints(100);

			// Act
			double defaultResult = defaultStrategy.applyWeight(mockVoting, mockCategory);
			double categoryFactorResult = categoryFactorStrategy.applyWeight(mockVoting, mockCategory);

			// Assert
			assertThat(categoryFactorResult)
					.isGreaterThan(defaultResult)
					.isEqualTo(50.0);
			assertThat(defaultResult).isEqualTo(5.0);
		}

		@Test
		@DisplayName("Ambas estrategias deben implementar VoteWeightingStrategy")
		void bothShouldImplementVoteWeightingStrategy() {
			assertThat(defaultStrategy).isInstanceOf(VoteWeightingStrategy.class);
			assertThat(categoryFactorStrategy).isInstanceOf(VoteWeightingStrategy.class);
		}
	}

	@Nested
	@DisplayName("Casos extremos y edge cases")
	class EdgeCasesTests {

		@Test
		@DisplayName("DefaultWeightingStrategy con Double.MAX_VALUE")
		void defaultStrategy_shouldHandleMaxValue() {
			// Arrange
			mockVoting.setScore(Integer.MAX_VALUE);

			// Act
			double result = defaultStrategy.applyWeight(mockVoting, mockCategory);

			// Assert
			assertThat(result).isEqualTo((double) Integer.MAX_VALUE);
		}

		@Test
		@DisplayName("CategoryFactorWeightingStrategy con Integer.MAX_VALUE points")
		void categoryFactorStrategy_shouldHandleMaxIntegerPoints() {
			// Arrange
			mockVoting.setScore(1);
			mockCategory.setTotalPoints(Integer.MAX_VALUE);

			// Act
			double result = categoryFactorStrategy.applyWeight(mockVoting, mockCategory);

			// Assert
			assertThat(result).isGreaterThan(1.0);
		}

		@Test
		@DisplayName("DefaultWeightingStrategy no debe ser afectado por la categoría")
		void defaultStrategy_shouldNotBeAffectedByAnyCategoryConfiguration() {
			// Arrange
			mockVoting.setScore(4);

			Category category1 = new Category();
			category1.setTotalPoints(10);

			Category category2 = new Category();
			category2.setTotalPoints(1000);

			// Act
			double result1 = defaultStrategy.applyWeight(mockVoting, category1);
			double result2 = defaultStrategy.applyWeight(mockVoting, category2);

			// Assert
			assertThat(result1).isEqualTo(result2).isEqualTo(4.0);
		}
	}

	@Nested
	@DisplayName("Comportamiento esperado del patrón Strategy")
	class StrategyPatternBehavior {

		@Test
		@DisplayName("Estrategias diferentes deben poder coexistir")
		void multipleDifferentStrategies_canCoexist() {
			VoteWeightingStrategy strategy1 = new DefaultWeightingStrategy();
			VoteWeightingStrategy strategy2 = new CategoryFactorWeightingStrategy();

			assertThat(strategy1).isNotSameAs(strategy2);
			assertThat(strategy1.key()).isNotEqualTo(strategy2.key());
		}

		@Test
		@DisplayName("Strategy debe permitir intercambiabilidad de implementaciones")
		void strategyShouldAllowInterchangeability() {
			// Simula la selección dinámica de estrategia
			mockVoting.setScore(10);
			mockCategory.setTotalPoints(50);

			VoteWeightingStrategy selectedStrategy = defaultStrategy;
			double result1 = selectedStrategy.applyWeight(mockVoting, mockCategory);

			selectedStrategy = categoryFactorStrategy;
			double result2 = selectedStrategy.applyWeight(mockVoting, mockCategory);

			assertThat(result1).isEqualTo(10.0);
			assertThat(result2).isEqualTo(50.0);
			assertThat(result1).isNotEqualTo(result2);
		}

		@Test
		@DisplayName("Todas las estrategias deben proporcionar una key única")
		void everyStrategy_shouldProvideUniqueKey() {
			String key1 = defaultStrategy.key();
			String key2 = categoryFactorStrategy.key();

			assertThat(key1).isNotNull().isNotEmpty();
			assertThat(key2).isNotNull().isNotEmpty();
			assertThat(key1).isNotEqualTo(key2);
		}

		@Test
		@DisplayName("applyWeight debe ser determinista (mismo input = mismo output)")
		void applyWeightShouldBeDeterministic() {
			// Arrange
			mockVoting.setScore(8);
			mockCategory.setTotalPoints(30);

			// Act
			double result1 = categoryFactorStrategy.applyWeight(mockVoting, mockCategory);
			double result2 = categoryFactorStrategy.applyWeight(mockVoting, mockCategory);
			double result3 = categoryFactorStrategy.applyWeight(mockVoting, mockCategory);

			// Assert
			assertThat(result1).isEqualTo(result2).isEqualTo(result3);
		}
	}
}
