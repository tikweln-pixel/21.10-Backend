package com.votify.dto;

import java.util.Date;

public class CategoryDto {

    private Long id;
    private String name;
    private Date timeInitial;
    private Date timeFinal;
    private Long eventId;
    private Integer reminderMinutes;

    public CategoryDto() {
    }

    public CategoryDto(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public CategoryDto(Long id, String name, Long eventId) {
        this.id = id;
        this.name = name;
        this.eventId = eventId;
    }

    public CategoryDto(Long id, String name, Date timeInitial, Date timeFinal, Long eventId) {
        this.id = id;
        this.name = name;
        this.timeInitial = timeInitial;
        this.timeFinal = timeFinal;
        this.eventId = eventId;
    }

    public CategoryDto(Long id, String name, Date timeInitial, Date timeFinal, Long eventId, Integer reminderMinutes) {
        this.id = id;
        this.name = name;
        this.timeInitial = timeInitial;
        this.timeFinal = timeFinal;
        this.eventId = eventId;
        this.reminderMinutes = reminderMinutes;
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

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Integer getReminderMinutes() {
        return reminderMinutes;
    }

    public void setReminderMinutes(Integer reminderMinutes) {
        this.reminderMinutes = reminderMinutes;
    }
}
