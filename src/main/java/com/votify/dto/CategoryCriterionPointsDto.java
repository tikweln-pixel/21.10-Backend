package com.votify.dto;

/**
 * DTO para la configuración de puntos máximos de un criterio dentro de una categoría.
 *
 * Corresponde a la pantalla "Configuración de puntos – Puntos Por Categoría"
 * del prototipo, donde cada slider representa un criterio con sus puntos máximos.
 */
public class CategoryCriterionPointsDto {

    private Long id;
    private Long categoryId;
    private Long criterionId;
    private String criterionName;
    private Integer maxPoints;

    public CategoryCriterionPointsDto() {
    }

    public CategoryCriterionPointsDto(Long id, Long categoryId, Long criterionId, String criterionName, Integer maxPoints) {
        this.id = id;
        this.categoryId = categoryId;
        this.criterionId = criterionId;
        this.criterionName = criterionName;
        this.maxPoints = maxPoints;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getCriterionId() {
        return criterionId;
    }

    public void setCriterionId(Long criterionId) {
        this.criterionId = criterionId;
    }

    public String getCriterionName() {
        return criterionName;
    }

    public void setCriterionName(String criterionName) {
        this.criterionName = criterionName;
    }

    public Integer getMaxPoints() {
        return maxPoints;
    }

    public void setMaxPoints(Integer maxPoints) {
        this.maxPoints = maxPoints;
    }
}
