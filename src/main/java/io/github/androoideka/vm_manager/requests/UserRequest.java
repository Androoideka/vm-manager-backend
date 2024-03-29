package io.github.androoideka.vm_manager.requests;

import io.github.androoideka.vm_manager.responses.PermissionListResponse;

public class UserRequest {
    private Long userId;
    private String email;
    private String password;
    private String name;
    private String surname;
    private PermissionListResponse permissionList;

    public UserRequest() {
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public PermissionListResponse getPermissionList() {
        return permissionList;
    }

    public void setPermissionList(PermissionListResponse permissionList) {
        this.permissionList = permissionList;
    }
}
