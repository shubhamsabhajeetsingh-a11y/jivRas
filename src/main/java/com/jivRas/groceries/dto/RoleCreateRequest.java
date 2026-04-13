package com.jivRas.groceries.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class RoleCreateRequest {

    @NotBlank(message = "Role name cannot be blank")
    private String roleName;

    @NotEmpty(message = "Permissions cannot be empty")
    private List<PermissionEntry> permissions;

    @Data
    public static class PermissionEntry {
        private String module;
        private String action;
        private boolean allowed;
    }
}
