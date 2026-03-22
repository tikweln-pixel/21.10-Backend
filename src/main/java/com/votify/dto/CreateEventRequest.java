package com.votify.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.votify.dto.json.CategoryNamesDeserializer;

import java.util.Date;
import java.util.List;

/**
 * Petición para crear un evento con varias categorías. El creador queda registrado
 * automáticamente como competidor en la categoría elegida.
 */
public class CreateEventRequest {

    @JsonAlias({"title"})
    private String name;

    @JsonAlias({"userId", "organizerId", "currentUserId"})
    private Long creatorUserId;

    @JsonAlias({"categories"})
    @JsonDeserialize(using = CategoryNamesDeserializer.class)
    private List<String> categoryNames;

    @JsonAlias({"creatorCategory", "selectedCategory"})
    private String creatorCategoryName;

    /** Inicio del evento (y de cada categoría creada); en JSON: cadena ISO-8601 o milisegundos desde la época Unix. */
    @JsonAlias({"startDate", "fechaInicio", "timeStart"})
    private Date timeInitial;

    /** Fin del evento (y de cada categoría creada). */
    @JsonAlias({"endDate", "fechaFin", "timeEnd"})
    private Date timeFinal;

    /** Recordatorio opcional (minutos antes del cierre) aplicado a cada categoría creada; p. ej. 60 = 1 hora. */
    @JsonAlias({"reminderMinutesBeforeClosing", "reminderBeforeCloseMinutes"})
    private Integer reminderMinutes;

    /** Si el front envía horas (p. ej. 1 para "1 hora"), se convierte a minutos al guardar. */
    @JsonAlias({"reminderHours", "reminderHoursBeforeClosing"})
    private Integer reminderHours;

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

    public Integer getReminderHours() {
        return reminderHours;
    }

    public void setReminderHours(Integer reminderHours) {
        this.reminderHours = reminderHours;
    }
}
