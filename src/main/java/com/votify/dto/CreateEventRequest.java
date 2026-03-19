package com.votify.dto;

import java.util.List;

/**
 * Request for creating an event with multiple categories. The creator is automatically
 * registered as a competitor in the chosen category.
 */
public class CreateEventRequest {

    private String name;
    private Long creatorUserId;
    private List<String> categoryNames;
    private String creatorCategoryName;

    public CreateEventRequest() {
    }

    public CreateEventRequest(String name, Long creatorUserId, List<String> categoryNames, String creatorCategoryName) {
        this.name = name;
        this.creatorUserId = creatorUserId;
        this.categoryNames = categoryNames;
        this.creatorCategoryName = creatorCategoryName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getCreatorUserId() {
        return creatorUserId;
    }

    public void setCreatorUserId(Long creatorUserId) {
        this.creatorUserId = creatorUserId;
    }

    public List<String> getCategoryNames() {
        return categoryNames;
    }

    public void setCategoryNames(List<String> categoryNames) {
        this.categoryNames = categoryNames;
    }

    public String getCreatorCategoryName() {
        return creatorCategoryName;
    }

    public void setCreatorCategoryName(String creatorCategoryName) {
        this.creatorCategoryName = creatorCategoryName;
    }
}
