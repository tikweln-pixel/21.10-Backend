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
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

@DisplayName("Strategy Pattern Tests")
public class VoteWeightingStrategyServiceTest {
	private Voting voting;
	private Category category;
	private VoteWeightingStrategy defaultStrategy;
	private VoteWeightingStrategy categoryFactorStrategy;

	@BeforeEach
	void setUp() {
		voting = new Voting();
		category = new Category();
		defaultStrategy = new DefaultWeightingStrategy();
		categoryFactorStrategy = new CategoryFactorWeightingStrategy();
	}

	@Nested
	class DefaultStrategyTests {
		@Test
		void testKeyDefault() {
			assertThat(defaultStrategy.key()).isEqualTo("default");
		}

		@Test
		void testScoreAsIs() {
			voting.setScore(Integer.valueOf(8));
			assertThat(defaultStrategy.applyWeight(voting, category)).isEqualTo(8);
		}

		@Test
		void testNullScore() {
			voting.setScore(null);
			assertThat(defaultStrategy.applyWeight(voting, category)).isEqualTo(0.0);
		}

		@Test
		void testZeroScore() {
			voting.setScore(Integer.valueOf(0));
			assertThat(defaultStrategy.applyWeight(voting, category)).isEqualTo(0);
		}

		@Test
		void testIgnoreCategoryNull() {
			voting.setScore(Integer.valueOf(7));
			category.setTotalPoints(null);
			assertThat(defaultStrategy.applyWeight(voting, category)).isEqualTo(7);
		}

		@Test
		void testIgnoreCategoryWithPoints() {
			voting.setScore(Integer.valueOf(5));
			category.setTotalPoints(Integer.valueOf(100));
			assertThat(defaultStrategy.applyWeight(voting, category)).isEqualTo(5);
		}

		@Test
		void testNullCategory() {
			voting.setScore(Integer.valueOf(6));
			assertThat(defaultStrategy.applyWeight(voting, null)).isEqualTo(6);
		}

		@Test
		void testNegativeScore() {
			voting.setScore(Integer.valueOf(-5));
			assertThat(defaultStrategy.applyWeight(voting, category)).isEqualTo(-5);
		}

		@Test
		void testExtremeValue() {
			voting.setScore(Integer.MAX_VALUE);
			assertThat(defaultStrategy.applyWeight(voting, category)).isEqualTo(Integer.MAX_VALUE);
		}
	}

	@Nested
	class CategoryFactorTests {
		@Test
		void testKeyFactor() {
			assertThat(categoryFactorStrategy.key()).isEqualTo("categoryFactor");
		}

		@Test
		void testFactor() {
			voting.setScore(Integer.valueOf(10));
			category.setTotalPoints(Integer.valueOf(50));
			assertThat(categoryFactorStrategy.applyWeight(voting, category)).isEqualTo(50.0);
		}

		@Test
		void testNullTotalPoints() {
			voting.setScore(Integer.valueOf(8));
			category.setTotalPoints(null);
			assertThat(categoryFactorStrategy.applyWeight(voting, category)).isEqualTo(8);
		}

		@Test
		void testZeroTotalPoints() {
			voting.setScore(Integer.valueOf(8));
			category.setTotalPoints(Integer.valueOf(0));
			assertThat(categoryFactorStrategy.applyWeight(voting, category)).isEqualTo(8);
		}

		@Test
		void testNullScore() {
			voting.setScore(null);
			category.setTotalPoints(Integer.valueOf(50));
			assertThat(categoryFactorStrategy.applyWeight(voting, category)).isEqualTo(0.0);
		}

		@Test
		void testNullCategory() {
			voting.setScore(Integer.valueOf(5));
			assertThat(categoryFactorStrategy.applyWeight(voting, null)).isEqualTo(5);
		}

		@Test
		void testLowPoints() {
			voting.setScore(Integer.valueOf(5));
			category.setTotalPoints(Integer.valueOf(10));
			assertThat(categoryFactorStrategy.applyWeight(voting, category)).isEqualTo(5);
		}

		@Test
		void testHighPoints() {
			voting.setScore(Integer.valueOf(8));
			category.setTotalPoints(Integer.valueOf(100));
			assertThat(categoryFactorStrategy.applyWeight(voting, category)).isEqualTo(80.0);
		}

		@Test
		void testFractional() {
			voting.setScore(Integer.valueOf(9));
			category.setTotalPoints(Integer.valueOf(15));
			assertThat(categoryFactorStrategy.applyWeight(voting, category)).isEqualTo(13.5);
		}

		@Test
		void testMaxValue() {
			voting.setScore(Integer.valueOf(1));
			category.setTotalPoints(Integer.MAX_VALUE);
			assertThat(categoryFactorStrategy.applyWeight(voting, category)).isGreaterThan(1.0);
		}
	}

	@Nested
	class ComparisonTests {
		@Test
		void testDifferentKeys() {
			assertThat(defaultStrategy.key()).isNotEqualTo(categoryFactorStrategy.key());
		}

		@Test
		void testSameWithoutCategory() {
			voting.setScore(Integer.valueOf(7));
			assertThat(defaultStrategy.applyWeight(voting, null)).isEqualTo(categoryFactorStrategy.applyWeight(voting, null));
		}

		@Test
		void testSameNoPoints() {
			voting.setScore(Integer.valueOf(6));
			category.setTotalPoints(null);
			assertThat(defaultStrategy.applyWeight(voting, category)).isEqualTo(categoryFactorStrategy.applyWeight(voting, category));
		}

		@Test
		void testAmplifies() {
			voting.setScore(Integer.valueOf(5));
			category.setTotalPoints(Integer.valueOf(100));
			double r1 = defaultStrategy.applyWeight(voting, category);
			double r2 = categoryFactorStrategy.applyWeight(voting, category);
			assertThat(r2).isGreaterThan(r1);
		}
	}

	@Nested
	class PatternTests {
		@Test
		void testCoexist() {
			VoteWeightingStrategy s1 = new DefaultWeightingStrategy();
			VoteWeightingStrategy s2 = new CategoryFactorWeightingStrategy();
			assertThat(s1).isNotSameAs(s2);
		}

		@Test
		void testSwitch() {
			voting.setScore(Integer.valueOf(10));
			category.setTotalPoints(Integer.valueOf(50));
			VoteWeightingStrategy s = defaultStrategy;
			double r1 = s.applyWeight(voting, category);
			s = categoryFactorStrategy;
			double r2 = s.applyWeight(voting, category);
			assertThat(r1).isEqualTo(10.0);
			assertThat(r2).isEqualTo(50.0);
		}

		@Test
		void testSelectByKey() {
			Map<String, VoteWeightingStrategy> map = new java.util.HashMap<>();
			map.put(defaultStrategy.key(), defaultStrategy);
			assertThat(map.get("default")).isNotNull();
		}

		@Test
		void testDeterministic() {
			voting.setScore(Integer.valueOf(7));
			category.setTotalPoints(Integer.valueOf(30));
			double r1 = categoryFactorStrategy.applyWeight(voting, category);
			double r2 = categoryFactorStrategy.applyWeight(voting, category);
			assertThat(r1).isEqualTo(r2);
		}
	}

	@Nested
	class EdgeCasesTests {
		@Test
		void testPrecision() {
			voting.setScore(Integer.valueOf(3));
			assertThat(defaultStrategy.applyWeight(voting, category)).isEqualTo(3);
		}

		@Test
		void testSmallValue() {
			voting.setScore(Integer.valueOf(1));
			category.setTotalPoints(Integer.valueOf(10));
			assertThat(categoryFactorStrategy.applyWeight(voting, category)).isEqualTo(1);
		}

		@Test
		void testLargeValue() {
			voting.setScore(Integer.valueOf(999999));
			assertThat(defaultStrategy.applyWeight(voting, category)).isEqualTo(999999);
		}

		@Test
		void testDefaultWithMaxCategory() {
			voting.setScore(Integer.valueOf(50));
			category.setTotalPoints(Integer.MAX_VALUE);
			assertThat(defaultStrategy.applyWeight(voting, category)).isEqualTo(50);
		}

		@Test
		void testCategoryFactorWithOne() {
			voting.setScore(Integer.valueOf(100));
			category.setTotalPoints(Integer.valueOf(1));
			assertThat(categoryFactorStrategy.applyWeight(voting, category)).isEqualTo(10.0);
		}

		@Test
		void testBothStrategiesWithZeroScore() {
			voting.setScore(Integer.valueOf(0));
			category.setTotalPoints(Integer.valueOf(50));
			double r1 = defaultStrategy.applyWeight(voting, category);
			double r2 = categoryFactorStrategy.applyWeight(voting, category);
			assertThat(r1).isZero();
			assertThat(r2).isZero();
		}

		@Test
		void testCategoryFactorEdgeCaseHighScore() {
			voting.setScore(Integer.valueOf(100));
			category.setTotalPoints(Integer.valueOf(2));
			assertThat(categoryFactorStrategy.applyWeight(voting, category)).isEqualTo(20.0);
		}

		@Test
		void testDefaultBehaviorConsistency() {
			voting.setScore(Integer.valueOf(42));
			assertThat(defaultStrategy.applyWeight(voting, category)).isEqualTo(42);
			assertThat(defaultStrategy.applyWeight(voting, category)).isEqualTo(42);
		}

		@Test
		void testCategoryFactorReturnsDouble() {
			voting.setScore(Integer.valueOf(3));
			category.setTotalPoints(Integer.valueOf(7));
			double result = categoryFactorStrategy.applyWeight(voting, category);
			assertThat(result).isCloseTo(2.1, within(0.0001));
		}

		@Test
		void testStrategyWithSinglePointCategory() {
			voting.setScore(Integer.valueOf(15));
			category.setTotalPoints(Integer.valueOf(1));
			assertThat(categoryFactorStrategy.applyWeight(voting, category)).isEqualTo(1.5);
		}

		@Test
		void testNegativeScoreWithCategoryFactor() {
			voting.setScore(Integer.valueOf(-10));
			category.setTotalPoints(Integer.valueOf(5));
			assertThat(categoryFactorStrategy.applyWeight(voting, category)).isEqualTo(-5.0);
		}

		@Test
		void testBothStrategiesReturnSameTypeDouble() {
			voting.setScore(Integer.valueOf(10));
			category.setTotalPoints(Integer.valueOf(3));
			assertThat(defaultStrategy.applyWeight(voting, category)).isInstanceOf(Double.class);
			assertThat(categoryFactorStrategy.applyWeight(voting, category)).isInstanceOf(Double.class);
		}

		@Test
		void testCategoryFactorWithZeroVotingMultipliesCorrectly() {
			voting.setScore(Integer.valueOf(0));
			category.setTotalPoints(Integer.valueOf(100));
			assertThat(categoryFactorStrategy.applyWeight(voting, category)).isZero();
		}

		@Test
		void testDefaultStrategyIgnoresAllCategoryProperties() {
			voting.setScore(Integer.valueOf(25));
			Category cat1 = new Category();
			cat1.setTotalPoints(Integer.valueOf(10));
			Category cat2 = new Category();
			cat2.setTotalPoints(Integer.valueOf(1000));
			assertThat(defaultStrategy.applyWeight(voting, cat1)).isEqualTo(defaultStrategy.applyWeight(voting, cat2));
		}

		@Test
		void testStrategyKeyValuesAreUniqueAndConsistent() {
			String key1 = defaultStrategy.key();
			String key2 = categoryFactorStrategy.key();
			assertThat(key1).isNotBlank();
			assertThat(key2).isNotBlank();
			assertThat(key1).isNotEqualTo(key2);
			assertThat(defaultStrategy.key()).isEqualTo(key1);
			assertThat(categoryFactorStrategy.key()).isEqualTo(key2);
		}
	}
}