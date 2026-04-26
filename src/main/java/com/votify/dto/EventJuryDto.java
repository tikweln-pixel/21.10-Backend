package com.votify.dto;

public class EventJuryDto {

    private Long id;
    private Long eventId;
    private Long userId;
    private String userName;
    private String userEmail;

    public EventJuryDto() {}

    public EventJuryDto(Long id, Long eventId, Long userId, String userName, String userEmail) {
        this.id = id;
        this.eventId = eventId;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
}
