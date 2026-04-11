package com.votify.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

@Entity
@Table(name = "categories")
public class Category {
    @Id
    private UUID id;
    private String name;
    private String slug;
    private String description;
    private boolean active;

    @ManyToOne(fetch = FetchType.LAZY)
    private Category parent;

    protected Category() {}

    private Category(UUID id, String name, String slug, String description, Category parent) {
        this.id = id;
        this.name = requireNonNull(name);
        this.slug = requireNonNull(slug);
        this.description = description;
        this.parent = parent;
        this.active = true;
    }

    public static Category create(String name, String slug, String description, Category parent) {
        return new Category(UUID.randomUUID(), name, slugify(name, slug), description, parent);
    }

    public void rename(String newName) {
        this.name = requireNonNull(newName);
        this.slug = slugify(newName, this.slug);
    }

    public void deactivate() { this.active = false; }

    // getters...

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category)) return false;
        Category other = (Category) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : System.identityHashCode(this);
    }

    // slugify(...) helper, validations...
}
