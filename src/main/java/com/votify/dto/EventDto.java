package com.votify.dto;

import java.util.Date;
import java.util.List;
//import java.util.ArrayList;
//import java.util.Collections;

public class EventDto {

    private Long id;
    private String name;
    private Date timeInitial;
    private Date timeFinal;

    private Long organizerId;
    private UserDto creator;
    private List<CategoryDto> categories;
    private List<ParticipantDto> participants;
    private List<ProjectDto> projects;
    private Integer reminderMinutes;
    private Integer reminderHours;

    public EventDto() {
    }

    public EventDto(Long id, String name, Date timeInitial, Date timeFinal, UserDto creator, List<CategoryDto> categories) {
        this.id = id;
        this.name = name;
        this.timeInitial = timeInitial;
        this.timeFinal = timeFinal;
        this.creator = creator;
        this.categories = categories;
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

    public Long getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(Long organizerId) {
        this.organizerId = organizerId;
    }

    public UserDto getCreator() {
        return creator;
    }

    public void setCreator(UserDto creator) {
        this.creator = creator;
    }

    public List<CategoryDto> getCategories() {
        return categories;
    }

    public void setCategories(List<CategoryDto> categories) {
        this.categories = categories;
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

    public List<ParticipantDto> getParticipants() {
        return participants;
    }

    public void setParticipants(List<ParticipantDto> participants) {
        this.participants = participants;
    }

    public List<ProjectDto> getProjects() {
        return projects;
    }

    public void setProjects(List<ProjectDto> projects) {
        this.projects = projects;
    }   

}
