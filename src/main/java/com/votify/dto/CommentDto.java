package com.votify.dto;

public class CommentDto {

    private Long id;
    private Long voterId;
    private String text;

    public CommentDto() {
    }

    public CommentDto(Long id, Long voterId, String text) {
        this.id = id;
        this.voterId = voterId;
        this.text = text;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVoterId() {
        return voterId;
    }

    public void setVoterId(Long voterId) {
        this.voterId = voterId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
