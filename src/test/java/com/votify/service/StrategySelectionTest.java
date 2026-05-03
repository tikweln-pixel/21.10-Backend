package com.votify.service;

import com.votify.adapters.strategy.CategoryFactorWeightingStrategy;
import com.votify.adapters.strategy.DefaultWeightingStrategy;
import com.votify.application.strategy.VoteWeightingStrategy;
import com.votify.entity.Category;
import com.votify.persistence.CategoryCriterionPointsRepository;
import com.votify.persistence.CategoryRepository;
import com.votify.persistence.CriterionRepository;
import com.votify.persistence.EvaluacionRepository;
import com.votify.persistence.VotingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests de integración para el patrón Strategy con CriterionService.
 * Verifica la selección dinámica y el uso de estrategias en el contexto del servicio.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Patrón Strategy: Integración con CriterionService — Tests")
class StrategySelectionTest {

	@Mock
	private CriterionRepository criterionRepository;

	@Mock
	private CategoryCriterionPointsRepository criterionPointsRepository;

	@Mock
	private VotingRepository votingRepository;

	@Mock
	private EvaluacionRepository evaluacionRepository;

	@Mock
	private CategoryRepository categoryRepository;

	private CriterionService criterionService;
	private Map<String, VoteWeightingStrategy> mockStrategies;
	private VoteWeightingStrategy defaultStrategy;
	private VoteWeightingStrategy categoryFactorStrategy;

	@BeforeEach
	void setUp() {
		// Crear instancias de estrategias
		defaultStrategy = new DefaultWeightingStrategy();
		categoryFactorStrategy = new CategoryFactorWeightingStrategy();

		// Crear mapa de estrategias simulando inyección de Spring
		mockStrategies = new HashMap<>();
		mockStrategies.put("default", defaultStrategy);
		mockStrategies.put("categoryFactor", categoryFactorStrategy);

		// Inicializar CriterionService con estrategias
		criterionService = new CriterionService(
				criterionRepository,
				criterionPointsRepository,
				votingRepository,
				evaluacionRepository,
				categoryRepository,
				mockStrategies
		);
	}

	@Nested
	@DisplayName("Obtención de estrategias — getStrategiesByKey()")
	class GetStrategiesByKeyTests {

		@Test
		@DisplayName("Debe retornar un mapa con ambas estrategias registradas")
		void shouldReturnMapWithBothStrategies() {
			// Act
			Map<String, VoteWeightingStrategy> strategies = criterionService.getStrategiesByKey();

			// Assert
			assertThat(strategies)
					.isNotNull()
					.hasSize(2)
					.containsKeys("default", "categoryFactor");
		}

		@Test
		@DisplayName("Debe mapear correctamente la estrategia por defecto")
		void shouldMapDefaultStrategyCorrectly() {
			// Act
			Map<String, VoteWeightingStrategy> strategies = criterionService.getStrategiesByKey();

			// Assert
			assertThat(strategies.get("default"))
					.isNotNull()
					.isInstanceOf(DefaultWeightingStrategy.class);
		}

		@Test
		@DisplayName("Debe mapear correctamente la estrategia de factor de categoría")
		void shouldMapCategoryFactorStrategyCorrectly() {
			// Act
			Map<String, VoteWeightingStrategy> strategies = criterionService.getStrategiesByKey();

			// Assert
			assertThat(strategies.get("categoryFactor"))
					.isNotNull()
					.isInstanceOf(CategoryFactorWeightingStrategy.class);
		}

		@Test
		@DisplayName("Cada key debe corresponder a una estrategia diferente")
		void eachKeyShouldCorrespondToDifferentStrategy() {
			// Act
			Map<String, VoteWeightingStrategy> strategies = criterionService.getStrategiesByKey();

			// Assert
			assertThat(strategies.get("default")).isNotSameAs(strategies.get("categoryFactor"));
		}
	}

	@Nested
	@DisplayName("Selección de estrategia para categoría — getStrategyForCategory()")
	class GetStrategyForCategoryTests {

		@Test
		@DisplayName("Debe devolver estrategia por defecto cuando categoría es null")
		void shouldReturnDefaultStrategy_whenCategoryIsNull() {
			// Act
			VoteWeightingStrategy strategy = criterionService.getStrategyForCategory(null);

			// Assert
			assertThat(strategy)
					.isNotNull()
					.isInstanceOf(DefaultWeightingStrategy.class);
		}

		@Test
		@DisplayName("Debe usar la política de ponderación explícita de la categoría si existe")
		void shouldUseCategoryExplicitWeightingPolicy() {
			// Arrange
			Category category = new Category();
			category.setWeightingPolicy("categoryFactor");

			// Act
			VoteWeightingStrategy strategy = criterionService.getStrategyForCategory(category);

			// Assert
			assertThat(strategy)
					.isNotNull()
					.isInstanceOf(CategoryFactorWeightingStrategy.class);
		}

		@Test
		@DisplayName("Debe usar estrategia de factor cuando categoría tiene totalPoints pero sin política explícita")
		void shouldUseCategoryFactorWhenCategoryHasTotalPointsButNoPolicyDefined() {
			// Arrange
			Category category = new Category();
			category.setTotalPoints(50);
			category.setWeightingPolicy(null);

			// Act
			VoteWeightingStrategy strategy = criterionService.getStrategyForCategory(category);

			// Assert
			assertThat(strategy)
					.isNotNull()
					.isInstanceOf(CategoryFactorWeightingStrategy.class);
		}

		@Test
		@DisplayName("Debe usar estrategia de factor cuando totalPoints es > 0 sin política")
		void shouldUseCategoryFactorWhenTotalPointsGreaterThanZero() {
			// Arrange
			Category category = new Category();
			category.setTotalPoints(100);
			category.setWeightingPolicy(null);

			// Act
			VoteWeightingStrategy strategy = criterionService.getStrategyForCategory(category);

			// Assert
			assertThat(strategy).isInstanceOf(CategoryFactorWeightingStrategy.class);
		}

		@Test
		@DisplayName("Debe devolver estrategia por defecto cuando totalPoints es cero o null sin política")
		void shouldReturnDefaultStrategyWhenTotalPointsIsZeroOrNull() {
			// Arrange
			Category category1 = new Category();
			category1.setTotalPoints(0);
			category1.setWeightingPolicy(null);

			Category category2 = new Category();
			category2.setTotalPoints(null);
			category2.setWeightingPolicy(null);

			// Act
			VoteWeightingStrategy strategy1 = criterionService.getStrategyForCategory(category1);
			VoteWeightingStrategy strategy2 = criterionService.getStrategyForCategory(category2);

			// Assert
			assertThat(strategy1).isInstanceOf(DefaultWeightingStrategy.class);
			assertThat(strategy2).isInstanceOf(DefaultWeightingStrategy.class);
		}

		@Test
		@DisplayName("Debe respetar la política explícita aunque categoría tenga totalPoints")
		void shouldRespectExplicitPolicy_evenIfCategoryHasTotalPoints() {
			// Arrange
			Category category = new Category();
			category.setTotalPoints(100);
			category.setWeightingPolicy("default");

			// Act
			VoteWeightingStrategy strategy = criterionService.getStrategyForCategory(category);

			// Assert
			assertThat(strategy).isInstanceOf(DefaultWeightingStrategy.class);
		}

		@Test
		@DisplayName("Debe devolver null si no hay estrategias disponibles")
		void shouldReturnNull_whenNoStrategiesAvailable() {
			// Arrange - CriterionService sin estrategias
			CriterionService serviceWithoutStrategies = new CriterionService(
					criterionRepository,
					criterionPointsRepository,
					votingRepository,
					evaluacionRepository,
					categoryRepository,
					new HashMap<>() // Mapa vacío
			);

			Category category = new Category();

			// Act
			VoteWeightingStrategy strategy = serviceWithoutStrategies.getStrategyForCategory(category);

			// Assert
			assertThat(strategy).isNull();
		}

		@Test
		@DisplayName("Debe manejar política inválida y usar fallback")
		void shouldHandleInvalidPolicy_andUseFallback() {
			// Arrange
			Category category = new Category();
			category.setWeightingPolicy("strategyNoExistente");

			// Act
			VoteWeightingStrategy strategy = criterionService.getStrategyForCategory(category);

			// Assert
			assertThat(strategy)
					.isNotNull()
					.isInstanceOf(DefaultWeightingStrategy.class);
		}

		@Test
		@DisplayName("Debe preferir política explícita sobre totalPoints")
		void shouldPreferExplicitPolicy_overTotalPoints() {
			// Arrange
			Category category = new Category();
			category.setWeightingPolicy("default");
			category.setTotalPoints(1000); // Alto valor que causaría usar categoryFactor

			// Act
			VoteWeightingStrategy strategy = criterionService.getStrategyForCategory(category);

			// Assert
			assertThat(strategy).isInstanceOf(DefaultWeightingStrategy.class);
		}

		@Test
		@DisplayName("Debe ser determinista para la misma categoría")
		void shouldBeDeterministic_forSameCategory() {
			// Arrange
			Category category = new Category();
			category.setTotalPoints(50);
			category.setWeightingPolicy("categoryFactor");

			// Act
			VoteWeightingStrategy strategy1 = criterionService.getStrategyForCategory(category);
			VoteWeightingStrategy strategy2 = criterionService.getStrategyForCategory(category);

			// Assert
			assertThat(strategy1).isSameAs(strategy2);
		}
	}

	@Nested
	@DisplayName("Fallback y casos especiales")
	class FallbackAndEdgeCasesTests {

		@Test
		@DisplayName("Debe usar 'default' como fallback cuando no hay estrategia registrada")
		void shouldUseDefault_asFallback_whenStrategyNotFound() {
			// Arrange
			Category category = new Category();
			category.setWeightingPolicy("estrategiaInexistente");

			// Act
			VoteWeightingStrategy strategy = criterionService.getStrategyForCategory(category);

			// Assert
			assertThat(strategy)
					.isNotNull()
					.isInstanceOf(DefaultWeightingStrategy.class);
		}

		@Test
		@DisplayName("Debe preferir política 'categoryFactor' si tiene totalPoints definido")
		void shouldPreferCategoryFactor_ifTotalPointsDefined() {
			// Arrange
			Category category = new Category();
			category.setWeightingPolicy(null);
			category.setTotalPoints(75);

			// Act
			VoteWeightingStrategy strategy = criterionService.getStrategyForCategory(category);

			// Assert
			assertThat(strategy).isInstanceOf(CategoryFactorWeightingStrategy.class);
		}

		@Test
		@DisplayName("Debe manejar categoría con política vacía")
		void shouldHandleEmptyPolicy() {
			// Arrange
			Category category = new Category();
			category.setWeightingPolicy("");
			category.setTotalPoints(null);

			// Act
			VoteWeightingStrategy strategy = criterionService.getStrategyForCategory(category);

			// Assert
			assertThat(strategy).isInstanceOf(DefaultWeightingStrategy.class);
		}

		@Test
		@DisplayName("Debe respetar totalPoints == 1")
		void shouldRespectTotalPointsEqualToOne() {
			// Arrange
			Category category = new Category();
			category.setTotalPoints(1);
			category.setWeightingPolicy(null);

			// Act
			VoteWeightingStrategy strategy = criterionService.getStrategyForCategory(category);

			// Assert
			assertThat(strategy).isInstanceOf(CategoryFactorWeightingStrategy.class);
		}
	}

	@Nested
	@DisplayName("Comportamiento del patrón Strategy aplicado")
	class StrategyPatternAppliedTests {

		@Test
		@DisplayName("Debe permitir cambiar de estrategia dinámicamente por categoría")
		void shouldAllowDynamicStrategySwitch_perCategory() {
			// Arrange
			Category categoryDefault = new Category();
			categoryDefault.setWeightingPolicy("default");

			Category categoryCategoryFactor = new Category();
			categoryCategoryFactor.setWeightingPolicy("categoryFactor");

			// Act
			VoteWeightingStrategy strategy1 = criterionService.getStrategyForCategory(categoryDefault);
			VoteWeightingStrategy strategy2 = criterionService.getStrategyForCategory(categoryCategoryFactor);

			// Assert
			assertThat(strategy1).isInstanceOf(DefaultWeightingStrategy.class);
			assertThat(strategy2).isInstanceOf(CategoryFactorWeightingStrategy.class);
			assertThat(strategy1).isNotSameAs(strategy2);
		}

		@Test
		@DisplayName("Estrategias seleccionadas dinámicamente deben tener comportamientos distintos")
		void dynamicallySelectedStrategies_shouldHaveDifferentBehaviors() {
			// Arrange
			Category categoryDefault = new Category();
			categoryDefault.setWeightingPolicy("default");
			categoryDefault.setTotalPoints(100);

			Category categoryCategoryFactor = new Category();
			categoryCategoryFactor.setWeightingPolicy("categoryFactor");
			categoryCategoryFactor.setTotalPoints(100);

			// Act
			VoteWeightingStrategy strategy1 = criterionService.getStrategyForCategory(categoryDefault);
			VoteWeightingStrategy strategy2 = criterionService.getStrategyForCategory(categoryCategoryFactor);

			// Assert
			assertThat(strategy1.key()).isEqualTo("default");
			assertThat(strategy2.key()).isEqualTo("categoryFactor");
			assertThat(strategy1.key()).isNotEqualTo(strategy2.key());
		}

		@Test
		@DisplayName("Todas las estrategias disponibles deben ser accesibles")
		void allAvailableStrategies_shouldBeAccessible() {
			// Act
			Map<String, VoteWeightingStrategy> strategies = criterionService.getStrategiesByKey();

			// Assert
			assertThat(strategies).hasSize(2);
			for (VoteWeightingStrategy strategy : strategies.values()) {
				assertThat(strategy.key()).isNotNull().isNotEmpty();
			}
		}

		@Test
		@DisplayName("Strategy pattern permite extensibilidad sin cambiar código existente")
		void strategyPattern_allowsExtensibility() {
			// Crear una nueva estrategia
			VoteWeightingStrategy customStrategy = new VoteWeightingStrategy() {
				@Override
				public String key() {
					return "custom";
				}

				@Override
				public double applyWeight(com.votify.entity.Voting vote, Category category) {
					return 42.0; // Estrategia custom
				}
			};

			// Agregar a las estrategias disponibles
			mockStrategies.put("custom", customStrategy);

			// Crear nueva instancia de criterionService con estrategia custom
			CriterionService extendedService = new CriterionService(
					criterionRepository,
					criterionPointsRepository,
					votingRepository,
					evaluacionRepository,
					categoryRepository,
					mockStrategies
			);

			// Verificar que pueda usar la estrategia custom
			Map<String, VoteWeightingStrategy> strategies = extendedService.getStrategiesByKey();
			assertThat(strategies).containsKey("custom");
			assertThat(strategies.get("custom").key()).isEqualTo("custom");
		}
	}
}
