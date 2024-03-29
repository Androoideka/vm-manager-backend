package io.github.androoideka.vm_manager.responses;

public class UserResponse {
    private Long userId;
    private String email;
    private String name;
    private String surname;
    private PermissionListResponse permissionListResponse;

    public UserResponse() {
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public PermissionListResponse getPermissionListResponse() {
        return permissionListResponse;
    }

    public void setPermissionListResponse(PermissionListResponse permissionListResponse) {
        this.permissionListResponse = permissionListResponse;
    }
}
