package com.votify.dto;

import java.util.List;

public class UserEventRolesDto {

    private Long userId;
    private String userName;
    private Long eventId;
    private boolean isJury;
    private String primaryRole;
    private List<CategoryRoleDto> categoryRoles;

    public UserEventRolesDto() {}

    public UserEventRolesDto(Long userId, String userName, Long eventId,
                              boolean isJury, String primaryRole,
                              List<CategoryRoleDto> categoryRoles) {
        this.userId = userId;
        this.userName = userName;
        this.eventId = eventId;
        this.isJury = isJury;
        this.primaryRole = primaryRole;
        this.categoryRoles = categoryRoles;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public boolean isJury() { return isJury; }
    public void setJury(boolean jury) { isJury = jury; }

    public String getPrimaryRole() { return primaryRole; }
    public void setPrimaryRole(String primaryRole) { this.primaryRole = primaryRole; }

    public List<CategoryRoleDto> getCategoryRoles() { return categoryRoles; }
    public void setCategoryRoles(List<CategoryRoleDto> categoryRoles) { this.categoryRoles = categoryRoles; }

    public static class CategoryRoleDto {
        private Long categoryId;
        private String categoryName;
        private String role;

        public CategoryRoleDto() {}

        public CategoryRoleDto(Long categoryId, String categoryName, String role) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            this.role = role;
        }

        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }
}
