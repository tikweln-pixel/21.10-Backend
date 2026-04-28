package com.votify.dto;

public class CriterionDto {

    private Long id;
    private String name;
    private Long categoryId;

    public CriterionDto() {
    }

    public CriterionDto(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public CriterionDto(Long id, String name, Long categoryId) {
        this.id = id;
        this.name = name;
        this.categoryId = categoryId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }
}
