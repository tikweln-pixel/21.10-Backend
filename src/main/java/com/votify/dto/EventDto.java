package com.votify.dto;

import java.util.Date;

public class EventDto {

    private Long id;
    private String name;
    private Date timeInitial;
    private Date timeFinal;

    public EventDto() {
    }

    public EventDto(Long id, String name, Date timeInitial, Date timeFinal) {
        //prueba de comentario

        this.id = id;
        this.name = name;
        this.timeInitial = timeInitial;
        this.timeFinal = timeFinal;
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
}
