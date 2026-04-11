package com.jivRas.groceries.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for the matrix PUT endpoint:
 * {@code PUT /api/role-permissions/matrix}
 */
@Data
public class RolePermissionRequest {

    @NotBlank(message = "role is required")
    private String role;

    @NotBlank(message = "module is required")
    private String module;

    @NotBlank(message = "action is required")
    private String action;

    private boolean allowed = true;
}
