package com.votify.persistence;

import com.votify.entity.Category;
import com.votify.entity.Event;
import com.votify.entity.VotingType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("CategoryRepository — Tests de persistencia")
class CategoryRepositoryTest {

    @Autowired TestEntityManager   em;
    @Autowired CategoryRepository  categoryRepository;

    private Event event1, event2;
    private Category cat1, cat2, cat3;

    @BeforeEach
    void setUp() {
        event1 = new Event("Hackathon 2026");
        event1.setTimeInitial(new Date(1_000_000L));
        event1.setTimeFinal(new Date(9_000_000L));
        em.persist(event1);

        event2 = new Event("Demo Day");
        em.persist(event2);

        cat1 = new Category("Jurado Experto", event1);
        cat1.setVotingType(VotingType.JURY_EXPERT);
        em.persist(cat1);

        cat2 = new Category("Voto Popular", event1);
        cat2.setVotingType(VotingType.POPULAR_VOTE);
        em.persist(cat2);

        cat3 = new Category("Otra Categoría", event2);
        em.persist(cat3);

        em.flush();
    }

    // ── findByEventId ──────────────────────────────────────────────────────

    @Test
    @DisplayName("findByEventId → retorna solo las categorías del evento especificado")
    void findByEventId_returnsOnlyCategoriesOfGivenEvent() {
        Long eId = event1.getId();
        List<Category> result = categoryRepository.findByEventId(Objects.requireNonNull(eId));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Category::getName)
                .containsExactlyInAnyOrder("Jurado Experto", "Voto Popular");
    }

    @Test
    @DisplayName("findByEventId → no incluye categorías de otros eventos")
    void findByEventId_doesNotIncludeCategoriesFromOtherEvents() {
        Long eId = event1.getId();
        List<Category> result = categoryRepository.findByEventId(Objects.requireNonNull(eId));

        assertThat(result).noneMatch(c -> c.getName().equals("Otra Categoría"));
    }

    @Test
    @DisplayName("findByEventId → retorna vacío para evento sin categorías")
    void findByEventId_returnsEmpty_forEventWithNoCategories() {
        Event emptyEvent = new Event("Sin Cats");
        em.persistAndFlush(emptyEvent);

        Long eId = emptyEvent.getId();
        List<Category> result = categoryRepository.findByEventId(Objects.requireNonNull(eId));
        assertThat(result).isEmpty();
    }

    // ── findById ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById → retorna categoría correcta")
    void findById_returnsCorrectCategory() {
        Long cId = cat1.getId();
        Optional<Category> result = categoryRepository.findById(Objects.requireNonNull(cId));

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Jurado Experto");
        assertThat(result.get().getVotingType()).isEqualTo(VotingType.JURY_EXPERT);
    }

    @Test
    @DisplayName("findById → retorna vacío para id inexistente")
    void findById_returnsEmpty_whenNotFound() {
        Optional<Category> result = categoryRepository.findById(9999L);
        assertThat(result).isEmpty();
    }

    // ── save ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("save → persiste nueva categoría con id generado")
    void save_persistsNewCategory() {
        Category newCat = new Category("Nueva Cat", event1);
        newCat.setVotingType(VotingType.JURY_EXPERT);

        Category saved = categoryRepository.save(newCat);

        assertThat(saved.getId()).isNotNull();
        Long sId = saved.getId();
        assertThat(categoryRepository.findById(Objects.requireNonNull(sId))).isPresent();
    }

    @Test
    @DisplayName("save → actualiza categoría existente")
    void save_updatesExistingCategory() {
        cat1.setVotingType(VotingType.POPULAR_VOTE);
        categoryRepository.save(cat1);

        Category reloaded = em.find(Category.class, cat1.getId());
        assertThat(reloaded.getVotingType()).isEqualTo(VotingType.POPULAR_VOTE);
    }

    // ── delete ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteById → elimina la categoría de la BD")
    void deleteById_removesCategory() {
        Long id = cat1.getId();
        categoryRepository.deleteById(Objects.requireNonNull(id));
        em.flush();
        em.clear();

        assertThat(categoryRepository.findById(Objects.requireNonNull(id))).isEmpty();
    }

    // ── findAll ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll → retorna todas las categorías persistidas")
    void findAll_returnsAllPersistedCategories() {
        List<Category> all = categoryRepository.findAll();
        assertThat(all).hasSizeGreaterThanOrEqualTo(3);
    }
}
