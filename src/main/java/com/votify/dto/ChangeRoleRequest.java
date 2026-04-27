package com.votify.dto;

import com.votify.entity.ParticipationRole;

public class ChangeRoleRequest {

    private ParticipationRole role;

    public ChangeRoleRequest() {}

    public ParticipationRole getRole() { return role; }
    public void setRole(ParticipationRole role) { this.role = role; }
}
