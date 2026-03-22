package com.votify.dto;

import java.util.Date;
import java.util.List;

/**
 * Petición para crear un evento con varias categorías. El creador queda registrado
 * automáticamente como competidor en la categoría elegida.
 */
public class CreateEventRequest {

    private String name;
    private Long creatorUserId;
    private List<String> categoryNames;
    private String creatorCategoryName;

    /** Inicio del evento (y de cada categoría creada); en JSON: cadena ISO-8601 o milisegundos desde la época Unix. */
    private Date timeInitial;

    /** Fin del evento (y de cada categoría creada). */
    private Date timeFinal;

    /** Recordatorio opcional (minutos antes del cierre) aplicado a cada categoría creada. */
    private Integer reminderMinutes;

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

    public Date getTimeInitial() {
        return timeInitial;
    }

    public void setTimeInitial(Date timeInitial) {
        this.timeInitial = timeInitial;
    }

    public Date getTimeFinal() {
        return timeFinal;
    }

    public void setTimeFinal(Date timeFinal) {
        this.timeFinal = timeFinal;
    }

    public Integer getReminderMinutes() {
        return reminderMinutes;
    }

    public void setReminderMinutes(Integer reminderMinutes) {
        this.reminderMinutes = reminderMinutes;
    }
}
